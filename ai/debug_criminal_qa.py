"""
debug_criminal_qa.py
=====================
criminal_qa 어댑터 하나만 로드해서, 문제가 됐던 질문으로 직접 생성해보고
1) RAG가 실제로 뭘 가져왔는지 (점수 + 청크 전체 내용, 40자 잘림 없이)
2) 모델에 들어간 프롬프트 전체
3) 모델이 실제로 생성한 원문(raw) 텍스트
를 전부 그대로 보여주는 1회성 진단 스크립트.

목적: "RAG가 엉뚱한 근거를 가져와서 그런지" vs "criminal_qa 어댑터 자체가
      (RAG 근거와 무관하게) 특정 문장을 계속 재생산하는지"를 구분하기 위함.
      main.py를 안 건드리고 따로 돌 수 있게 분리함 - 로딩 시간을 아끼려고
      administrative_qa/civil_qa는 아예 로드하지 않는다 (ko_llama3 그룹에서
      criminal_qa 어댑터 하나만).

사용법 (ai/ 디렉터리, weights/·indexes/ 있는 서버에서):
    docker exec -it <컨테이너> python debug_criminal_qa.py
    docker exec -it <컨테이너> python debug_criminal_qa.py "다른 질문 텍스트"

환경변수:
    DEBUG_K=10          RAG에서 몇 개 후보까지 볼지 (기본 10, 프로덕션 RAG_TOP_K=3보다 넉넉히)
    DEBUG_TEMPERATURE    QA_GEN_KWARGS의 temperature를 잠깐 바꿔서 테스트하고 싶을 때
"""

import os
import sys

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import PeftModel

from rag_chain import retrieve_context, format_context_block, build_rag_messages

BASE_MODEL_PATH = "beomi/Llama-3-Open-Ko-8B-Instruct-preview"
ADAPTER_PATH = "weights/criminal_qa"
LEGAL_TYPE = "criminal"
DEFAULT_INSTRUCTION = "질문에 대해 정확하고 간결하게 답변하시오."

DEFAULT_QUESTION = "중고로 산 물건이 가짜였어요"

DEBUG_K = int(os.environ.get("DEBUG_K", "10"))
TEMPERATURE = float(os.environ.get("DEBUG_TEMPERATURE", "0.1"))


def main():
    question = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_QUESTION

    if not os.path.isdir(ADAPTER_PATH):
        print(f"[오류] {ADAPTER_PATH} 가 없습니다. 이 스크립트는 weights/ 가 있는 서버(컨테이너)에서 돌려야 합니다.")
        sys.exit(1)

    # ── 1) RAG 후보 전체(내용 잘림 없이) ─────────────────────────────
    print("=" * 80)
    print(f"[질문] {question!r}")
    print(f"[RAG] legal_type={LEGAL_TYPE}, k={DEBUG_K} 로 검색")
    print("=" * 80)

    retrieved = retrieve_context(LEGAL_TYPE, question, k=DEBUG_K)
    if not retrieved:
        print("(검색 결과 없음 - threshold를 넘는 후보가 아예 없었음)")
    for item in retrieved:
        score = item.get("_dense_score")
        score_str = f"{score:.4f}" if score is not None else "N/A(BM25 단독 매칭)"
        print(f"\n--- [{item['rank']}] score={score_str} {item.get('law_name','')} {item.get('article_no','')} "
              f"{item.get('case_num','')} ({item.get('docu_type','')}) ---")
        print(item["_content"])  # 40자 잘림 없이 전체 출력

    has_copier = any("복사기" in item["_content"] for item in retrieved)
    print("\n" + "=" * 80)
    print(f"['복사기' 문자열이 검색된 근거 청크 안에 있는가?] -> {has_copier}")
    if has_copier:
        copier_scores = [item.get("_dense_score") for item in retrieved if "복사기" in item["_content"]]
        print(f"['복사기' 포함된 후보의 dense score] -> {copier_scores} "
              f"(현재 threshold={os.environ.get('RAG_SCORE_THRESHOLD', '1.0(기본값)')}, 낮을수록 더 유사하다고 판단됨)")
        print("   -> .env에 RAG_SCORE_THRESHOLD=<이 값보다 작은 수> 를 넣고 컨테이너를 재시작하면 "
              "이 후보를 걸러낼 수 있는지 테스트해볼 수 있음 (단, 다른 정상 케이스까지 걸러질 수 있으니 "
              "여러 질문으로 같이 확인 필요).")
    print("=" * 80)

    # 실제 프로덕션에서 criminal_qa에 쓰이는 RAG_TOP_K(main.py 기준 3)로 만든 컨텍스트도
    # 별도로 확인 (위 DEBUG_K=10 후보 중 상위 3개만 실제 프롬프트에 들어감)
    context_block_prod = format_context_block(retrieved[:3])
    messages = build_rag_messages(DEFAULT_INSTRUCTION, question, context_block_prod)

    # ── 2) 모델 로드 (criminal_qa 어댑터만) ──────────────────────────
    print("\n[모델 로딩 중...]")
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL_PATH)
    tokenizer.pad_token = tokenizer.eos_token
    tokenizer.pad_token_id = tokenizer.eos_token_id

    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.bfloat16,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_use_double_quant=True,
    )
    base_model = AutoModelForCausalLM.from_pretrained(
        BASE_MODEL_PATH, quantization_config=bnb_config, device_map={"": 0},
    )
    model = PeftModel.from_pretrained(base_model, ADAPTER_PATH, adapter_name="criminal_qa")
    model.set_adapter("criminal_qa")
    model.eval()

    # ── 3) 실제 프롬프트 전체 출력 ────────────────────────────────────
    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    print("\n" + "=" * 80)
    print("[모델에 들어가는 프롬프트 전체]")
    print("=" * 80)
    print(prompt)

    # ── 4) 생성 (temperature 등은 main.py의 QA_GEN_KWARGS와 동일한 값을 기본값으로 사용) ─
    inputs = tokenizer(prompt, return_tensors="pt", truncation=True, max_length=4096).to(model.device)
    input_len = inputs["input_ids"].shape[-1]

    eos_ids = [tokenizer.eos_token_id]
    eot_id = tokenizer.convert_tokens_to_ids("<|eot_id|>")
    if eot_id is not None and eot_id != tokenizer.unk_token_id:
        eos_ids.append(eot_id)

    with torch.no_grad():
        output_ids = model.generate(
            **inputs,
            eos_token_id=eos_ids,
            pad_token_id=tokenizer.pad_token_id,
            max_new_tokens=2560,
            do_sample=True,
            top_p=0.9,
            temperature=TEMPERATURE,
            repetition_penalty=1.05,
        )

    raw_full_decoded = tokenizer.decode(output_ids[0], skip_special_tokens=True)
    new_tokens_only = tokenizer.decode(output_ids[0][input_len:], skip_special_tokens=True)

    print("\n" + "=" * 80)
    print("[생성된 새 토큰만 디코딩한 결과 (프로덕션 추출 방식)]")
    print("=" * 80)
    print(new_tokens_only.strip())

    print("\n" + "=" * 80)
    print("[참고: 전체 시퀀스를 디코딩한 결과 (프롬프트 포함, 비교용)]")
    print("=" * 80)
    print(raw_full_decoded)

    print("\n" + "=" * 80)
    print("[판정 힌트]")
    print(f"- '복사기'가 실제 RAG 근거 청크 안에 있었는가: {has_copier}")
    print("  -> True면: RAG가 실제로 그런 근거를 가져왔다는 뜻 (도메인 매칭/인덱스 커버리지 문제)")
    print("  -> False면: 근거 자료에 없는데 모델이 만들어냈다는 뜻 -> criminal_qa 어댑터 자체의")
    print("     암기/과적합(overfitting) 가능성이 높음. 같은 질문을 temperature를 올려서")
    print("     (DEBUG_TEMPERATURE=0.7 등) 여러 번 돌려보고, 매번 '컬러복사기'가 나오는지")
    print("     확인해보면 확실해짐 - 매번 똑같이 나오면 RAG 문제가 아니라 어댑터가")
    print("     그 문장을 거의 암기하고 있다는 뜻.")
    print("=" * 80)


if __name__ == "__main__":
    main()

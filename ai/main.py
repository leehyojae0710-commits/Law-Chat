"""
main.py
=======
기존 server.py의 모델 로딩/어댑터 라우팅 구조를 그대로 유지하면서,
'qa' 태스크에 한해 RAG(하이브리드 검색 -> 근거주의 프롬프트) 단계를 추가한 서버.

바뀐 부분은 딱 세 곳뿐입니다.
  1) lifespan에서 db_loader 인덱스(FAISS+BM25)를 미리 로딩 (rag_chain.preload_all_retrievers)
  2) _generate_with_adapter(task="qa")일 때 build_prompt() 대신
     retrieve_context() -> build_rag_messages() 경로를 탐
  3) ChatResponse / DomainAnswer에 "sources"(검색된 근거 문서 목록) 필드 추가
     -> 사용자가 "이 답변이 어떤 법조문/판례에 근거했는지" 프론트에서 그대로 보여줄 수 있음

sum(요약) 태스크는 이미 원문이 입력으로 들어오므로 검색이 필요 없어
기존 server.py 로직(QA_SYSTEM_MSG 등)을 그대로 씁니다.

실행:
    uvicorn main:app --host 0.0.0.0 --port 8000
"""

import os
import logging
import time
import uuid
import torch
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import PeftModel

from router import classify_domains, is_legal_question, DOMAIN_LABELS
from rag_chain import retrieve_context, format_context_block, build_rag_messages, preload_all_retrievers

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(message)s")
log = logging.getLogger("legal-chatbot")

# ─────────────────────────────────────────────────────────────
# 모델 그룹 정의 (server.py와 동일)
# ─────────────────────────────────────────────────────────────
MODEL_GROUPS = {
    "ko_llama3": {
        "base_model_path": "beomi/Llama-3-Open-Ko-8B-Instruct-preview",
        "pre_quantized": False,
        "adapters": {
            "criminal_qa": "weights/criminal_qa",
            "criminal_sum": "weights/criminal_sum",
            "administrative_qa": "weights/administrative_qa",
            "administrative_sum": "weights/administrative_sum",
        },
    },
    "llama31": {
        "base_model_path": "unsloth/meta-llama-3.1-8b-instruct-bnb-4bit",
        "pre_quantized": True,
        "adapters": {
            "civil_qa": "weights/civil_qa",
            # "intellectual_property_qa": "weights/intellectual_property_qa",
        },
    },
}

# RAG를 적용할 legal_type 목록. 해당 legal_type의 인덱스가
# ./indexes/<legal_type>/ 에 미리 만들어져 있어야 한다 (db_loader.py 참고).
RAG_LEGAL_TYPES = ["civil", "criminal", "administrative"]
RAG_TOP_K = 5

# sum 태스크용 템플릿은 server.py와 동일하게 유지 (검색 불필요)
SUM_USER_TEMPLATE = '''지시 : {instruction}\n\n입력 : "{source}"\n'''
SUM_SYSTEM_MSG = "주어진 지시대로 텍스트에 대한 요약을 생성합니다\n\n"
SUM_GEN_KWARGS = dict(
    max_new_tokens=2560, do_sample=True, top_p=0.9, temperature=0.5,
    repetition_penalty=1.15, no_repeat_ngram_size=3,
)

# qa 태스크는 RAG 컨텍스트가 프롬프트에 들어가는 만큼 조금 더 보수적으로(temperature 낮게) 생성
QA_GEN_KWARGS = dict(
    max_new_tokens=2560, do_sample=True, top_p=0.9, temperature=0.1,
    repetition_penalty=1.15, no_repeat_ngram_size=3,
)

DEFAULT_QA_INSTRUCTION = "질문에 대해 정확하고 간결하게 답변하시오."
DEFAULT_SUM_INSTRUCTION = "다음 텍스트를 핵심 내용 중심으로 요약하시오."

SYNTHESIS_GROUP = "ko_llama3"
SYNTHESIS_SYSTEM_MSG = "여러 법 분야의 답변을 종합하여 이해하기 쉬운 하나의 답변을 작성합니다\n\n"
SYNTHESIS_USER_TEMPLATE = '''다음은 하나의 사용자 질문에 대해 서로 다른 법 분야 관점에서 생성된 답변들입니다.

사용자 질문 : "{question}"

{domain_answers}

위 내용을 참고하여, 법률 지식이 없는 사용자가 이해하기 쉽도록 하나의 자연스러운 답변으로 통합해서 작성하시오.
중복되는 내용은 제거하고, 각 법적 절차/쟁점이 실제로 어떤 순서나 관계로 연결되는지 설명하시오.
'''
SYNTHESIS_GEN_KWARGS = dict(
    max_new_tokens=3840, do_sample=True, top_p=0.9, temperature=0.3,
    repetition_penalty=1.2, no_repeat_ngram_size=3,
)

state = {"groups": {}, "adapter_to_group": {}}

# ─────────────────────────────────────────────────────────────
# /chat/simple, /chat/simple/reset 은 주석에 명시된 대로 "정식 기능이 아니라
# 테스트 용도"입니다. Spring 백엔드는 chat_sessions/chat_messages 테이블로
# 세션/이력을 정식으로 관리하므로 이 인메모리 세션 엔드포인트를 쓰면 안 됩니다
# (서버 재시작 시 대화 소실, DB와 상태 불일치 위험).
# AWS 운영 환경에서 실수로 호출되는 걸 막기 위해 기본은 비활성화하고,
# 로컬에서 직접 테스트할 때만 ENABLE_TEST_ENDPOINTS=true 로 켜서 씁니다.
# ─────────────────────────────────────────────────────────────
ENABLE_TEST_ENDPOINTS = os.environ.get("ENABLE_TEST_ENDPOINTS", "false").lower() == "true"

# ─────────────────────────────────────────────────────────────
# 임시 테스트용: 로그인 없이 세션ID만으로 대화를 이어가는 간단 챗봇
# (DB 없이 인메모리 캐시 -> 서버 재시작하면 대화 내역은 사라짐. 정식 기능이 아니라
#  "질문-답변을 이어서 테스트"하기 위한 용도로만 사용할 것)
# ─────────────────────────────────────────────────────────────
SESSION_TTL_SECONDS = 60 * 60 * 2  # 2시간 이상 활동 없으면 다음 요청 때 정리
SESSION_MAX_TURNS_IN_PROMPT = 4    # 프롬프트에 포함할 최근 대화 턴(질문+답변 쌍) 수
SESSION_CACHE: dict[str, dict] = {}  # session_id -> {"history": [...], "last_seen": float}

# router.classify_domains()가 반환하는 1등 도메인의 신뢰도 점수만으로는 "법률 질문
# 자체가 맞는지"를 안정적으로 걸러낼 수 없어서(도메인별 독립 판단 방식의 한계),
# router.is_legal_question()으로 별도 게이트를 먼저 통과시킨다.
OFF_TOPIC_REPLY = (
    "죄송하지만 저는 형사·민사·행정 등 법률 관련 질문에만 답변할 수 있는 챗봇입니다. "
    "겪고 계신 상황이나 궁금하신 법률 문제를 조금 더 구체적으로 적어주시면 도와드리겠습니다."
)


def _cleanup_sessions() -> None:
    now = time.time()
    expired = [sid for sid, s in SESSION_CACHE.items() if now - s["last_seen"] > SESSION_TTL_SECONDS]
    for sid in expired:
        del SESSION_CACHE[sid]


def _get_session(session_id: str | None) -> tuple[str, dict]:
    _cleanup_sessions()
    if session_id and session_id in SESSION_CACHE:
        SESSION_CACHE[session_id]["last_seen"] = time.time()
        return session_id, SESSION_CACHE[session_id]
    new_id = session_id or str(uuid.uuid4())
    SESSION_CACHE[new_id] = {"history": [], "last_seen": time.time()}
    return new_id, SESSION_CACHE[new_id]


class SimpleChatRequest(BaseModel):
    session_id: str | None = None  # 없으면 새 세션 발급
    text: str


class SimpleChatResponse(BaseModel):
    session_id: str
    answer: str
    legal_type_ko: str | None = None


def _load_model_group(group_name: str, config: dict):
    base_model_path = config["base_model_path"]
    pre_quantized = config.get("pre_quantized", False)

    available = {name: path for name, path in config["adapters"].items() if os.path.isdir(path)}
    if not available:
        log.info(f"[{group_name}] 로드할 어댑터가 없어 이 그룹은 건너뜁니다 ({base_model_path}).")
        return None

    log.info(f"[{group_name}] 베이스 모델 로드 중: {base_model_path} (pre_quantized={pre_quantized})")

    tokenizer = AutoTokenizer.from_pretrained(base_model_path)
    tokenizer.pad_token = tokenizer.eos_token
    tokenizer.pad_token_id = tokenizer.eos_token_id

    if pre_quantized:
        base_model = AutoModelForCausalLM.from_pretrained(base_model_path, device_map={"": 0})
    else:
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_compute_dtype=torch.bfloat16,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_use_double_quant=True,
        )
        base_model = AutoModelForCausalLM.from_pretrained(
            base_model_path, quantization_config=bnb_config, device_map={"": 0},
        )

    log.info(f"[{group_name}] 발견된 어댑터: {list(available.keys())}")

    model = None
    for name, path in available.items():
        if model is None:
            log.info(f"[{group_name}] 첫 어댑터 로드: {name} <- {path}")
            model = PeftModel.from_pretrained(base_model, path, adapter_name=name)
        else:
            log.info(f"[{group_name}] 추가 어댑터 로드: {name} <- {path}")
            model.load_adapter(path, adapter_name=name)

    model.eval()
    return {"model": model, "tokenizer": tokenizer, "adapters": set(available.keys())}


@asynccontextmanager
async def lifespan(app: FastAPI):
    for group_name, config in MODEL_GROUPS.items():
        try:
            loaded = _load_model_group(group_name, config)
        except Exception:
            log.exception(f"[{group_name}] 로드 실패 -> 이 그룹은 사용할 수 없습니다.")
            loaded = None
            # 실패한 시도가 남긴 부분 할당(단편화 유발)을 정리해서 다음 그룹이
            # 조금이라도 더 깨끗한 상태에서 큰 연속 블록을 잡을 수 있게 한다.
            torch.cuda.empty_cache()

        if loaded is not None:
            state["groups"][group_name] = loaded
            for adapter_name in loaded["adapters"]:
                state["adapter_to_group"][adapter_name] = group_name

        # 그룹 하나(베이스 모델 로드 + 어댑터 병합)가 끝날 때마다 캐싱 할당자가
        # 쥐고 있는 미사용 블록을 반납해서, 다음 그룹이 큰 연속 메모리를
        # 요청할 때 단편화로 인한 가짜 OOM("계산상 여유는 있는데 할당 실패")을 줄인다.
        torch.cuda.empty_cache()

    if not state["groups"]:
        raise RuntimeError(
            "로드된 모델 그룹이 하나도 없습니다. MODEL_GROUPS 설정과 weights/ 폴더를 확인하세요."
        )

    log.info(f"모델 로드 완료. 로드된 그룹: {list(state['groups'].keys())}")
    log.info(f"사용 가능한 전체 어댑터: {sorted(state['adapter_to_group'].keys())}")

    # RAG 인덱스 미리 로딩 (없으면 경고만 남기고 계속 진행 -> 해당 legal_type은 검색 없이 답변)
    preload_all_retrievers(RAG_LEGAL_TYPES)

    try:
        classify_domains("테스트")
        is_legal_question("테스트")
        log.info("도메인 분류기 워밍업 완료.")
    except Exception:
        log.exception("도메인 분류기 워밍업 실패 (첫 요청 시 재시도됩니다)")

    yield
    state.clear()


app = FastAPI(title="Korean Legal Chatbot API (RAG)", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)


class ChatRequest(BaseModel):
    task: str
    legal_type: str
    text: str
    instruction: str | None = None


class SourceDoc(BaseModel):
    rank: int
    law_name: str
    article_no: str
    docu_type: str
    case_num: str
    url: str = ""  # Spring 백엔드 LegalSourceResponse.url 매핑용 (rag_chain.build_source_url 참고)


class ChatResponse(BaseModel):
    task: str
    legal_type: str
    adapter_used: str
    model_group: str
    answer: str
    sources: list[SourceDoc] = []   # qa + RAG 인덱스가 있을 때만 채워짐


class AutoChatRequest(BaseModel):
    text: str
    instruction: str | None = None


class DomainAnswer(BaseModel):
    legal_type: str
    legal_type_ko: str
    score: float
    adapter_used: str
    model_group: str
    answer: str
    sources: list[SourceDoc] = []


class AutoChatResponse(BaseModel):
    text: str
    detected_domains: list[DomainAnswer]
    unavailable_domains: list[str]
    answer: str


def build_prompt(tokenizer, task: str, instruction: str, text: str) -> str:
    """sum 태스크 전용 (qa는 build_rag_prompt를 따로 씀)."""
    user_message = SUM_USER_TEMPLATE.format(instruction=instruction, source=text)
    messages = [
        {"role": "system", "content": SUM_SYSTEM_MSG},
        {"role": "user", "content": f"{user_message}\n\n"},
    ]
    return tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)


def build_rag_prompt(tokenizer, legal_type: str, instruction: str, question: str, k: int = RAG_TOP_K):
    """qa 태스크 전용. 검색 -> 컨텍스트 조합 -> 근거주의 프롬프트 구성까지 한번에 수행."""
    try:
        retrieved = retrieve_context(legal_type, question, k=k)
    except FileNotFoundError:
        log.warning(f"[RAG] '{legal_type}' 인덱스가 없어 검색 없이 진행합니다 (환각 위험 있음).")
        retrieved = []

    context_block = format_context_block(retrieved)
    messages = build_rag_messages(instruction, question, context_block)
    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    return prompt, retrieved


def _get_eos_ids(tokenizer) -> list[int]:
    ids = [tokenizer.eos_token_id]
    eot_id = tokenizer.convert_tokens_to_ids("<|eot_id|>")
    if eot_id is not None and eot_id != tokenizer.unk_token_id and eot_id not in ids:
        ids.append(eot_id)
    return ids


def _run_generation(model, tokenizer, prompt: str, gen_kwargs: dict) -> str:
    inputs = tokenizer(prompt, return_tensors="pt", truncation=True, max_length=4096).to(model.device)
    with torch.no_grad():
        output_ids = model.generate(
            **inputs,
            eos_token_id=_get_eos_ids(tokenizer),
            pad_token_id=tokenizer.pad_token_id,
            **gen_kwargs,
        )
    decoded = tokenizer.decode(output_ids[0], skip_special_tokens=True)
    return " ".join(decoded.split("assistant")[1:]).strip()


def _generate_with_adapter(
    task: str, legal_type: str, text: str, instruction: str | None
) -> tuple[str, str, str, list[dict]]:
    """반환값에 sources(검색된 근거 문서)가 하나 추가된 것 외에는 server.py와 동일한 인터페이스."""
    adapter_name = f"{legal_type}_{task}"
    group_name = state["adapter_to_group"].get(adapter_name)
    if group_name is None:
        raise HTTPException(
            404,
            f"'{adapter_name}' 어댑터가 로드되어 있지 않습니다. "
            f"사용 가능: {sorted(state['adapter_to_group'].keys())}",
        )

    group = state["groups"][group_name]
    model = group["model"]
    tokenizer = group["tokenizer"]

    sources: list[dict] = []
    if task == "qa":
        instruction = instruction or DEFAULT_QA_INSTRUCTION
        prompt, sources = build_rag_prompt(tokenizer, legal_type, instruction, text)
        gen_kwargs = QA_GEN_KWARGS
    else:
        instruction = instruction or DEFAULT_SUM_INSTRUCTION
        prompt = build_prompt(tokenizer, task, instruction, text)
        gen_kwargs = SUM_GEN_KWARGS

    model.set_adapter(adapter_name)
    answer = _run_generation(model, tokenizer, prompt, gen_kwargs)
    return adapter_name, group_name, answer, sources


@app.get("/health")
def health():
    groups_info = {
        name: {
            "base_model": MODEL_GROUPS[name]["base_model_path"],
            "adapters_loaded": sorted(g["adapters"]),
        }
        for name, g in state.get("groups", {}).items()
    }
    return {"status": "ok", "groups": groups_info, "rag_legal_types": RAG_LEGAL_TYPES}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if not state.get("groups"):
        raise HTTPException(503, "모델이 아직 로딩 중입니다.")
    if req.task not in ("qa", "sum"):
        raise HTTPException(400, "task는 'qa' 또는 'sum'이어야 합니다.")

    adapter_name, model_group, answer, sources = _generate_with_adapter(
        req.task, req.legal_type, req.text, req.instruction
    )

    return ChatResponse(
        task=req.task, legal_type=req.legal_type,
        adapter_used=adapter_name, model_group=model_group, answer=answer,
        sources=[SourceDoc(**s) for s in sources],
    )


@app.post("/chat/auto", response_model=AutoChatResponse)
def chat_auto(req: AutoChatRequest):
    if not state.get("groups"):
        raise HTTPException(503, "모델이 아직 로딩 중입니다.")
    if not req.text or not req.text.strip():
        raise HTTPException(400, "text가 비어 있습니다.")

    ranked = classify_domains(req.text)

    domain_answers: list[DomainAnswer] = []
    unavailable: list[str] = []
    for legal_type, score in ranked:
        adapter_key = f"{legal_type}_qa"
        if adapter_key in state["adapter_to_group"]:
            adapter_name, model_group, answer, sources = _generate_with_adapter(
                "qa", legal_type, req.text, req.instruction
            )
            domain_answers.append(
                DomainAnswer(
                    legal_type=legal_type,
                    legal_type_ko=DOMAIN_LABELS[legal_type],
                    score=round(score, 4),
                    adapter_used=adapter_name,
                    model_group=model_group,
                    answer=answer,
                    sources=[SourceDoc(**s) for s in sources],
                )
            )
        else:
            unavailable.append(legal_type)

    if not domain_answers:
        detected_ko = ", ".join(DOMAIN_LABELS[d] for d in unavailable) or "알 수 없음"
        return AutoChatResponse(
            text=req.text, detected_domains=[], unavailable_domains=unavailable,
            answer=(
                f"이 질문은 '{detected_ko}' 관련 질문으로 보이나, "
                f"해당 분야의 답변 모델이 아직 준비되지 않았습니다."
            ),
        )

    if len(domain_answers) == 1:
        final_answer = domain_answers[0].answer
    else:
        if SYNTHESIS_GROUP not in state["groups"]:
            final_answer = domain_answers[0].answer
        else:
            synth_group = state["groups"][SYNTHESIS_GROUP]
            synth_model = synth_group["model"]
            synth_tokenizer = synth_group["tokenizer"]

            domain_answers_text = "\n\n".join(
                f"[{da.legal_type_ko} 관점]\n{da.answer}" for da in domain_answers
            )
            synth_user_message = SYNTHESIS_USER_TEMPLATE.format(
                question=req.text, domain_answers=domain_answers_text
            )
            messages = [
                {"role": "system", "content": SYNTHESIS_SYSTEM_MSG},
                {"role": "user", "content": f"{synth_user_message}\n\n"},
            ]
            synth_prompt = synth_tokenizer.apply_chat_template(
                messages, tokenize=False, add_generation_prompt=True
            )
            with synth_model.disable_adapter():
                final_answer = _run_generation(synth_model, synth_tokenizer, synth_prompt, SYNTHESIS_GEN_KWARGS)

    return AutoChatResponse(
        text=req.text, detected_domains=domain_answers,
        unavailable_domains=unavailable, answer=final_answer,
    )


@app.post("/chat/simple", response_model=SimpleChatResponse)
def chat_simple(req: SimpleChatRequest):
    """로그인 없이 session_id 하나로만 대화를 이어가는 간이 엔드포인트.
    도메인 분류 -> 해당 어댑터로 QA 생성까지는 /chat/auto와 동일한 로직을 재사용하되,
    - 여러 도메인 통합(synthesis) 과정은 생략하고 최고 점수 도메인 하나로만 답변
    - 세션의 최근 대화 이력을 프롬프트에 덧붙여 '이어지는 대화'처럼 응답하도록 함

    ⚠️ 정식 기능 아님. Spring 백엔드는 이 엔드포인트를 호출하지 말 것 (/chat/auto 사용).
    """
    if not ENABLE_TEST_ENDPOINTS:
        raise HTTPException(
            403,
            "/chat/simple은 테스트 전용 엔드포인트입니다. "
            "정식 연동은 /chat/auto를 사용하세요. "
            "로컬 테스트가 필요하면 ENABLE_TEST_ENDPOINTS=true 로 서버를 실행하세요.",
        )
    if not state.get("groups"):
        raise HTTPException(503, "모델이 아직 로딩 중입니다.")
    if not req.text or not req.text.strip():
        raise HTTPException(400, "질문을 입력해주세요.")

    session_id, session = _get_session(req.session_id)
    history = session["history"]

    # 1단계: 법률 질문인지 먼저 게이트 통과 (도메인 분류보다 먼저 -> 무관한 요청은
    # 도메인 분류/RAG 검색/LLM 생성까지 갈 필요 없이 여기서 바로 컷)
    legal_ok, gate_score = is_legal_question(req.text)
    if not legal_ok:
        answer = OFF_TOPIC_REPLY
        history.append({"role": "user", "content": req.text})
        history.append({"role": "assistant", "content": answer})
        return SimpleChatResponse(session_id=session_id, answer=answer, legal_type_ko=None)

    # 2단계: 어떤 법 분야인지 분류
    ranked = classify_domains(req.text)

    # 점수 순으로 훑으면서 실제 로드된 어댑터가 있는 첫 도메인을 사용
    legal_type = None
    for lt, _score in ranked:
        if f"{lt}_qa" in state["adapter_to_group"]:
            legal_type = lt
            break

    if legal_type is None:
        top_ko = DOMAIN_LABELS.get(ranked[0][0], "알 수 없음")
        answer = f"'{top_ko}' 관련 질문으로 보이지만, 해당 분야는 아직 준비된 답변 모델이 없습니다."
        history.append({"role": "user", "content": req.text})
        history.append({"role": "assistant", "content": answer})
        return SimpleChatResponse(session_id=session_id, answer=answer, legal_type_ko=top_ko)

    instruction = DEFAULT_QA_INSTRUCTION
    if history:
        recent = history[-(SESSION_MAX_TURNS_IN_PROMPT * 2):]
        history_text = "\n".join(
            f"{'사용자' if h['role'] == 'user' else '어시스턴트'}: {h['content']}" for h in recent
        )
        instruction = (
            "아래는 지금까지 나눈 대화 이력입니다. 이 맥락을 참고해서 방금 들어온 질문에 이어서 답변하시오 "
            "(이전 답변과 중복되는 설명은 반복하지 말 것).\n\n"
            f"[이전 대화]\n{history_text}\n\n{DEFAULT_QA_INSTRUCTION}"
        )

    _adapter_name, _model_group, answer, _sources = _generate_with_adapter(
        "qa", legal_type, req.text, instruction
    )

    history.append({"role": "user", "content": req.text})
    history.append({"role": "assistant", "content": answer})

    return SimpleChatResponse(
        session_id=session_id, answer=answer, legal_type_ko=DOMAIN_LABELS.get(legal_type)
    )


@app.post("/chat/simple/reset")
def chat_simple_reset(session_id: str):
    """테스트 중 대화를 처음부터 다시 시작하고 싶을 때 세션 이력만 지움.
    ⚠️ /chat/simple과 마찬가지로 테스트 전용.
    """
    if not ENABLE_TEST_ENDPOINTS:
        raise HTTPException(403, "/chat/simple/reset은 테스트 전용 엔드포인트입니다.")
    SESSION_CACHE.pop(session_id, None)
    return {"status": "ok"}

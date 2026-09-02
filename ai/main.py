"""
main.py
=======
법률 챗봇(QA + RAG) + 판례요약(KoBART) 통합 서버.

역할이 완전히 분리된 두 기능을 한 프로세스(한 GPU)에서 같이 띄운다.
  1) 챗봇 (/chat, /chat/auto, /chat/simple*)
     - Llama 계열 베이스 모델 + 도메인별 LoRA 어댑터(criminal_qa, administrative_qa, civil_qa 등)
     - RAG(FAISS+BM25) 검색 -> 근거주의 프롬프트
     - 예전에는 "_sum" LoRA 어댑터(criminal_sum 등)로 요약도 처리했지만, 판례요약은
       KoBART 전용 모델(precedent_summarizer)로 대체되어 더 이상 사용하지 않는다.
       -> MODEL_GROUPS에서 "_sum" 어댑터 항목 제거, task는 "qa"만 허용.
  2) 판례요약 (/summarize/precedent)
     - KoBART 파인튜닝 체크포인트(precedent_summarizer 패키지, summarizer.py가 래핑)
     - 챗봇용 어댑터와는 완전히 다른 모델이라 별도로 로드/추론한다.
     - 판례 검색(백엔드 PrecedentController) 결과의 fullText를 받아 요약 + 쉬운 설명을 반환.

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
from summarizer import load_kobart_once, is_ready as kobart_ready, summarize_precedent

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(message)s")
log = logging.getLogger("legal-chatbot")

# ─────────────────────────────────────────────────────────────
# 모델 그룹 정의: QA 어댑터만 남김 ("_sum" 어댑터는 KoBART로 대체되어 제거)
# ─────────────────────────────────────────────────────────────
MODEL_GROUPS = {
    "ko_llama3": {
        "base_model_path": "beomi/Llama-3-Open-Ko-8B-Instruct-preview",
        "pre_quantized": False,
        "adapters": {
            "criminal_qa": "weights/criminal_qa",
            "administrative_qa": "weights/administrative_qa",
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

# qa 태스크는 RAG 컨텍스트가 프롬프트에 들어가는 만큼 조금 더 보수적으로(temperature 낮게) 생성
QA_GEN_KWARGS = dict(
    max_new_tokens=2560, do_sample=True, top_p=0.9, temperature=0.1,
    repetition_penalty=1.15, no_repeat_ngram_size=3,
)

DEFAULT_QA_INSTRUCTION = "질문에 대해 정확하고 간결하게 답변하시오."

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
# /chat/simple, /chat/simple/reset 은 정식 기능이 아니라 테스트 용도입니다.
# Spring 백엔드는 chat_sessions/chat_messages 테이블로 세션/이력을 정식으로
# 관리하므로 이 인메모리 세션 엔드포인트를 쓰면 안 됩니다.
# ─────────────────────────────────────────────────────────────
ENABLE_TEST_ENDPOINTS = os.environ.get("ENABLE_TEST_ENDPOINTS", "false").lower() == "true"

SESSION_TTL_SECONDS = 60 * 60 * 2  # 2시간 이상 활동 없으면 다음 요청 때 정리
SESSION_MAX_TURNS_IN_PROMPT = 4    # 프롬프트에 포함할 최근 대화 턴(질문+답변 쌍) 수
SESSION_CACHE: dict[str, dict] = {}  # session_id -> {"history": [...], "last_seen": float}

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
            torch.cuda.empty_cache()

        if loaded is not None:
            state["groups"][group_name] = loaded
            for adapter_name in loaded["adapters"]:
                state["adapter_to_group"][adapter_name] = group_name

        torch.cuda.empty_cache()

    if not state["groups"]:
        log.warning(
            "로드된 QA 어댑터가 하나도 없습니다. MODEL_GROUPS 설정과 weights/ 폴더를 확인하세요. "
            "(챗봇 엔드포인트는 503을 반환하지만, /summarize/precedent 는 별도로 계속 시도합니다)"
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

    # KoBART 판례요약 모델 로드 (체크포인트 없으면 경고만 남기고 계속 -> /summarize/precedent 는 503)
    try:
        load_kobart_once()
    except Exception:
        log.exception("[kobart] 로드 실패 -> /summarize/precedent 는 사용할 수 없습니다.")
        torch.cuda.empty_cache()

    yield
    state.clear()


app = FastAPI(title="Korean Legal Chatbot API (RAG + KoBART Summarizer)", lifespan=lifespan)

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
    sources: list[SourceDoc] = []   # RAG 인덱스가 있을 때만 채워짐


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


# ─────────────────────────────────────────────────────────────
# 판례요약(KoBART) 요청/응답 스키마
# ─────────────────────────────────────────────────────────────
class SummarizeRequest(BaseModel):
    text: str                 # 판례 원문 (백엔드 Precedent.fullText)
    plain: bool = True        # true면 법률 용어 쉬운 풀이(explain_for_layperson)도 같이 반환


class SummarizeResponse(BaseModel):
    summary: str               # KoBART 원본 요약
    plain_summary: str | None  # plain=true일 때만 채워짐 (용어 풀이 버전)


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


def _generate_qa(legal_type: str, text: str, instruction: str | None) -> tuple[str, str, str, list[dict]]:
    """QA 전용 생성. 반환: (adapter_name, group_name, answer, sources)"""
    adapter_name = f"{legal_type}_qa"
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

    instruction = instruction or DEFAULT_QA_INSTRUCTION
    prompt, sources = build_rag_prompt(tokenizer, legal_type, instruction, text)

    model.set_adapter(adapter_name)
    answer = _run_generation(model, tokenizer, prompt, QA_GEN_KWARGS)
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
    return {
        "status": "ok",
        "groups": groups_info,
        "rag_legal_types": RAG_LEGAL_TYPES,
        "kobart_ready": kobart_ready(),
    }


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if not state.get("groups"):
        raise HTTPException(503, "모델이 아직 로딩 중입니다.")
    if req.task != "qa":
        raise HTTPException(
            400,
            "task는 'qa'만 지원합니다. 판례요약은 /summarize/precedent 를 사용하세요.",
        )

    adapter_name, model_group, answer, sources = _generate_qa(req.legal_type, req.text, req.instruction)

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
            adapter_name, model_group, answer, sources = _generate_qa(legal_type, req.text, req.instruction)
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
    """⚠️ 정식 기능 아님. Spring 백엔드는 이 엔드포인트를 호출하지 말 것 (/chat/auto 사용)."""
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

    legal_ok, gate_score = is_legal_question(req.text)
    if not legal_ok:
        answer = OFF_TOPIC_REPLY
        history.append({"role": "user", "content": req.text})
        history.append({"role": "assistant", "content": answer})
        return SimpleChatResponse(session_id=session_id, answer=answer, legal_type_ko=None)

    ranked = classify_domains(req.text)

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

    _adapter_name, _model_group, answer, _sources = _generate_qa(legal_type, req.text, instruction)

    history.append({"role": "user", "content": req.text})
    history.append({"role": "assistant", "content": answer})

    return SimpleChatResponse(
        session_id=session_id, answer=answer, legal_type_ko=DOMAIN_LABELS.get(legal_type)
    )


@app.post("/chat/simple/reset")
def chat_simple_reset(session_id: str):
    """⚠️ /chat/simple과 마찬가지로 테스트 전용."""
    if not ENABLE_TEST_ENDPOINTS:
        raise HTTPException(403, "/chat/simple/reset은 테스트 전용 엔드포인트입니다.")
    SESSION_CACHE.pop(session_id, None)
    return {"status": "ok"}


# ─────────────────────────────────────────────────────────────
# 판례요약 (KoBART) — 챗봇 어댑터와 완전히 별개의 모델/엔드포인트.
# 백엔드 PrecedentController -> PrecedentService에서 판례 상세(fullText)를 이
# 엔드포인트로 보내 AI 요약을 받아온다.
# ─────────────────────────────────────────────────────────────
@app.post("/summarize/precedent", response_model=SummarizeResponse)
def summarize_precedent_endpoint(req: SummarizeRequest):
    if not kobart_ready():
        raise HTTPException(
            503,
            "판례요약(KoBART) 모델이 로딩되지 않았습니다. "
            "KOBART_CHECKPOINT 경로에 체크포인트가 있는지 확인하세요.",
        )
    if not req.text or not req.text.strip():
        raise HTTPException(400, "요약할 판례 원문이 비어 있습니다.")

    summary, plain_summary = summarize_precedent(req.text, plain=req.plain)
    return SummarizeResponse(summary=summary, plain_summary=plain_summary)

"""
법률 챗봇 API 서버
- 두 개의 서로 다른 베이스 모델을 동시에 로드해서 서빙한다.
  1) "ko_llama3" 그룹: beomi/Llama-3-Open-Ko-8B-Instruct-preview (직접 4bit 양자화)
     - 형사법(criminal), 행정법(administrative) LoRA 어댑터
  2) "llama31" 그룹: unsloth/meta-llama-3.1-8b-instruct-bnb-4bit (이미 4bit로 저장된 repo)
     - 민사법(civil), 지식재산권법(intellectual_property) QA LoRA 어댑터
     - (민사/지재 SUM은 pko-t5 기반이라 이 서버에서는 아직 미지원)
- 각 그룹 내부에서는 기존처럼 set_adapter()로 어댑터만 스위칭한다.
- 요청이 들어오면 요청된 어댑터가 어느 그룹 소속인지 찾아서 그 그룹의 모델로 라우팅한다.

주의:
- "llama31" 그룹의 베이스 모델(unsloth bnb-4bit)은 이미 4bit로 양자화되어 저장된
  체크포인트라서, 로드 시 BitsAndBytesConfig를 별도로 씌우지 않는다
  (config.json에 양자화 설정이 이미 포함되어 있어 transformers가 자동 인식함).
  반면 "ko_llama3" 그룹은 풀프리시전 원본이라 로드 시 직접 4bit 양자화를 적용한다.
- 두 개의 8B 모델을 각각 4bit로 올리면 VRAM이 대략 6~7GB x 2 = 12~14GB 내외 필요하다.
  현재 GPU가 16GB(RTX 2000 Ada)라 여유가 크지 않으니, 서버 재시작 전 기존 프로세스가
  GPU를 점유하고 있지 않은지 nvidia-smi로 꼭 확인할 것.

실행:
    uvicorn server:app --host 0.0.0.0 --port 8000
"""

import os
import logging
import torch
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import PeftModel

from router import classify_domains, DOMAIN_LABELS

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(message)s")
log = logging.getLogger("legal-chatbot")

# ─────────────────────────────────────────────────────────────
# 모델 그룹 정의: 그룹마다 베이스 모델이 다르고, 그 위에 얹는 어댑터도 다르다.
# 각 어댑터의 실제 adapter_config.json을 직접 열어서 base_model_name_or_path와
# 폴더명을 확인한 뒤 아래 경로/이름을 맞췄다 (2026-08-18 확인).
#
# 나중에 어댑터가 추가/변경되면 adapter_config.json의 base_model_name_or_path를
# 꼭 다시 확인한 뒤 맞는 그룹에 등록할 것.
# ─────────────────────────────────────────────────────────────
MODEL_GROUPS = {
    "ko_llama3": {
        "base_model_path": "beomi/Llama-3-Open-Ko-8B-Instruct-preview",
        "pre_quantized": False,  # 원본 풀프리시전 -> 로드 시 직접 4bit 양자화 적용
        "adapters": {
            "criminal_qa": "weights/criminal_qa",
            "criminal_sum": "weights/criminal_sum",
            "administrative_qa": "weights/administrative_qa",
            "administrative_sum": "weights/administrative_sum",
        },
    },
    "llama31": {
        "base_model_path": "unsloth/meta-llama-3.1-8b-instruct-bnb-4bit",
        "pre_quantized": True,  # 이미 4bit로 저장된 체크포인트 -> BitsAndBytesConfig 재적용 안 함
        "adapters": {
            "civil_qa": "weights/civil_qa",
            # intellectual_property_qa: 아직 실물 어댑터 미확보. 폴더가 생기면 아래 주석 해제.
            # "intellectual_property_qa": "weights/intellectual_property_qa",
            # civil_sum, intellectual_property_sum은 pko-t5 기반이라 여기서 다루지 않음
        },
    },
}

# 원본 학습 코드(inference.py)와 동일한 프롬프트 템플릿 및 생성 파라미터
QA_USER_TEMPLATE = '''지시 : {instruction}\n\n주어진 질문에 적합한 내용의 답변을 생성합니다. 질문 : "{question}"\n'''
QA_SYSTEM_MSG = "주어진 지시대로 질문에 대한 답변을 생성합니다\n\n"
QA_GEN_KWARGS = dict(
    max_new_tokens=2560, do_sample=True, top_p=0.9, temperature=0.1,
    repetition_penalty=1.15, no_repeat_ngram_size=3,
)

SUM_USER_TEMPLATE = '''지시 : {instruction}\n\n입력 : "{source}"\n'''
SUM_SYSTEM_MSG = "주어진 지시대로 텍스트에 대한 요약을 생성합니다\n\n"
SUM_GEN_KWARGS = dict(
    max_new_tokens=2560, do_sample=True, top_p=0.9, temperature=0.5,
    repetition_penalty=1.15, no_repeat_ngram_size=3,
)

DEFAULT_QA_INSTRUCTION = "질문에 대해 정확하고 간결하게 답변하시오."
DEFAULT_SUM_INSTRUCTION = "다음 텍스트를 핵심 내용 중심으로 요약하시오."

# 여러 도메인 답변을 하나로 종합할 때 쓰는 프롬프트 (어댑터 없이 베이스 모델로 실행)
# 종합은 항상 "ko_llama3" 그룹(기본으로 항상 로드되는 그룹)의 베이스로 수행한다.
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


def _load_model_group(group_name: str, config: dict):
    """그룹 하나(베이스 모델 1개 + 그 소속 어댑터들)를 로드."""
    base_model_path = config["base_model_path"]
    pre_quantized = config.get("pre_quantized", False)

    # 그룹 내 실제 존재하는 어댑터 폴더만 필터링
    available = {name: path for name, path in config["adapters"].items() if os.path.isdir(path)}
    if not available:
        log.info(f"[{group_name}] 로드할 어댑터가 없어 이 그룹은 건너뜁니다 ({base_model_path}).")
        return None

    log.info(f"[{group_name}] 베이스 모델 로드 중: {base_model_path} (pre_quantized={pre_quantized})")

    tokenizer = AutoTokenizer.from_pretrained(base_model_path)
    tokenizer.pad_token = tokenizer.eos_token
    tokenizer.pad_token_id = tokenizer.eos_token_id

    if pre_quantized:
        # 이미 4bit로 저장된 체크포인트: config.json에 양자화 설정이 내장되어 있으므로
        # BitsAndBytesConfig를 별도로 넘기지 않는다 (넘기면 이중 양자화로 충돌/에러 가능).
        base_model = AutoModelForCausalLM.from_pretrained(
            base_model_path,
            device_map={"": 0},
        )
    else:
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_compute_dtype=torch.bfloat16,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_use_double_quant=True,
        )
        base_model = AutoModelForCausalLM.from_pretrained(
            base_model_path,
            quantization_config=bnb_config,
            device_map={"": 0},
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

        if loaded is not None:
            state["groups"][group_name] = loaded
            for adapter_name in loaded["adapters"]:
                state["adapter_to_group"][adapter_name] = group_name

    if not state["groups"]:
        raise RuntimeError(
            "로드된 모델 그룹이 하나도 없습니다. MODEL_GROUPS 설정과 weights/ 폴더를 확인하세요."
        )

    log.info(f"모델 로드 완료. 로드된 그룹: {list(state['groups'].keys())}")
    log.info(f"사용 가능한 전체 어댑터: {sorted(state['adapter_to_group'].keys())}")

    try:
        classify_domains("테스트")
        log.info("도메인 분류기 워밍업 완료.")
    except Exception:
        log.exception("도메인 분류기 워밍업 실패 (첫 요청 시 재시도됩니다)")

    yield
    state.clear()


app = FastAPI(title="Korean Legal Chatbot API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    task: str
    legal_type: str
    text: str
    instruction: str | None = None


class ChatResponse(BaseModel):
    task: str
    legal_type: str
    adapter_used: str
    model_group: str
    answer: str


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


class AutoChatResponse(BaseModel):
    text: str
    detected_domains: list[DomainAnswer]
    unavailable_domains: list[str]
    answer: str


def build_prompt(tokenizer, task: str, instruction: str, text: str) -> str:
    if task == "qa":
        user_message = QA_USER_TEMPLATE.format(instruction=instruction, question=text)
        system_msg = QA_SYSTEM_MSG
    else:
        user_message = SUM_USER_TEMPLATE.format(instruction=instruction, source=text)
        system_msg = SUM_SYSTEM_MSG

    messages = [
        {"role": "system", "content": system_msg},
        {"role": "user", "content": f"{user_message}\n\n"},
    ]
    return tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)


def _get_eos_ids(tokenizer) -> list[int]:
    """
    Llama-3 계열 채팅 템플릿은 turn이 끝날 때 <|eot_id|> 토큰으로 끝난다.
    그런데 tokenizer.eos_token_id는 보통 <|end_of_text|>를 가리켜서,
    이것만 종료 조건으로 주면 모델이 답변을 다 마친 뒤에도 멈추지 못하고
    max_new_tokens까지 억지로 생성하게 된다 (그 결과 repetition_penalty와
    맞물려 이모지/외국어/깨진 토큰이 뒤섞인 답변이 나옴).
    존재하는 경우 <|eot_id|>도 종료 토큰으로 함께 등록한다.
    """
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


def _generate_with_adapter(task: str, legal_type: str, text: str, instruction: str | None) -> tuple[str, str, str]:
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

    instruction = instruction or (DEFAULT_QA_INSTRUCTION if task == "qa" else DEFAULT_SUM_INSTRUCTION)
    prompt = build_prompt(tokenizer, task, instruction, text)

    model.set_adapter(adapter_name)
    gen_kwargs = QA_GEN_KWARGS if task == "qa" else SUM_GEN_KWARGS
    answer = _run_generation(model, tokenizer, prompt, gen_kwargs)
    return adapter_name, group_name, answer


@app.get("/health")
def health():
    groups_info = {
        name: {
            "base_model": MODEL_GROUPS[name]["base_model_path"],
            "adapters_loaded": sorted(g["adapters"]),
        }
        for name, g in state.get("groups", {}).items()
    }
    return {"status": "ok", "groups": groups_info}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if not state.get("groups"):
        raise HTTPException(503, "모델이 아직 로딩 중입니다.")

    if req.task not in ("qa", "sum"):
        raise HTTPException(400, "task는 'qa' 또는 'sum'이어야 합니다.")

    adapter_name, model_group, answer = _generate_with_adapter(
        req.task, req.legal_type, req.text, req.instruction
    )

    return ChatResponse(
        task=req.task, legal_type=req.legal_type,
        adapter_used=adapter_name, model_group=model_group, answer=answer,
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
            adapter_name, model_group, answer = _generate_with_adapter(
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
                )
            )
        else:
            unavailable.append(legal_type)

    if not domain_answers:
        detected_ko = ", ".join(DOMAIN_LABELS[d] for d in unavailable) or "알 수 없음"
        return AutoChatResponse(
            text=req.text,
            detected_domains=[],
            unavailable_domains=unavailable,
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
        text=req.text,
        detected_domains=domain_answers,
        unavailable_domains=unavailable,
        answer=final_answer,
    )
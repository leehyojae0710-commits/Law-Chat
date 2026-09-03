"""KoBART 기반 판례요약 모델 추론 모듈.

`README_판결요약.md` / `AI모델사용매뉴얼_판례요약.docx` 기준:
- 모델: BART 계열(KoBART) 파인튜닝
- 학습 결과물은 HuggingFace `save_pretrained()` 형식으로 저장됨
  (예: "result/2024-02-21 10:36/checkpoint-26606")
- 평가지표: ROUGE-L

`summarize()` / `explain_for_layperson()` 은 tokenizer/model 을 인자로 주입받는
구조라서, 실제 KoBART 가중치 없이도(FakeTokenizer/FakeModel로 대체) 로직만 따로
테스트할 수 있다. transformers/torch 는 실제 로딩 함수(`load_summarizer`) 안에서만
import 하므로, 테스트 실행 시 무거운 의존성이 필요 없다.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Protocol


class SummarizerTokenizer(Protocol):
    """HF PreTrainedTokenizer 와 호환되는 최소 인터페이스."""

    def __call__(
        self, text: str, return_tensors: str = "pt", truncation: bool = True, max_length: int = 1024
    ) -> Any: ...

    def decode(self, token_ids: Any, skip_special_tokens: bool = True) -> str: ...


class SummarizerModel(Protocol):
    """HF PreTrainedModel 과 호환되는 최소 인터페이스."""

    def generate(self, **kwargs: Any) -> Any: ...


@dataclass
class GenerationConfig:
    """요약문 생성 하이퍼파라미터. KoBART 요약 태스크에서 흔히 쓰는 기본값."""

    max_length: int = 128
    min_length: int = 8
    num_beams: int = 4
    no_repeat_ngram_size: int = 3
    length_penalty: float = 1.2


def load_summarizer(
    checkpoint_path: str, tokenizer_path: str | None = None, device: str | None = None
):
    """실제 학습된 체크포인트를 로드한다. (transformers, torch 필요 - production 전용)

    Args:
        checkpoint_path: 학습 결과 디렉토리 (모델 가중치 - config.json/model.safetensors 등)
            (예: "result/2024-02-21 10:36/checkpoint-26606")
        tokenizer_path: 토크나이저가 저장된 경로. HF Trainer는 토크나이저를 매
            checkpoint-* 폴더가 아니라 학습 결과 루트 폴더에 한 번만 저장하는 경우가 많다.
            미지정 시 checkpoint_path 와 동일한 경로에서 찾는다. 로컬 경로가 없으면
            학습에 사용한 베이스 모델의 HF 허브 repo id(예: "gogamza/kobart-base-v2")를
            넣어도 된다 - 단, 파인튜닝에 실제로 쓰인 것과 다른 vocab이면 결과가 깨진다.
        device: "cuda" | "cpu". 미지정 시 GPU 사용 가능하면 자동으로 cuda 선택.

    Returns:
        (tokenizer, model) 튜플. 둘 다 summarize() 에 그대로 넘기면 된다.
    """
    import torch
    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

    device = device or ("cuda" if torch.cuda.is_available() else "cpu")
    tokenizer = AutoTokenizer.from_pretrained(tokenizer_path or checkpoint_path)
    model = AutoModelForSeq2SeqLM.from_pretrained(checkpoint_path).to(device)
    model.eval()
    return tokenizer, model


def _clean_input(text: str) -> str:
    """판례 원문 전처리: 개행/중복 공백 정리 (summ_contxt 형식에 맞춤)."""
    return " ".join(text.split())


def summarize(
    text: str,
    tokenizer: SummarizerTokenizer,
    model: SummarizerModel,
    config: GenerationConfig | None = None,
) -> str:
    """판례 원문(text) -> 요약문 한 줄 생성.

    tokenizer/model 은 HF 인터페이스를 따르는 어떤 객체든 주입 가능하다.
    테스트에서는 FakeTokenizer/FakeModel 로 대체해 무거운 의존성 없이 검증한다.
    """
    if not text or not text.strip():
        raise ValueError("요약할 판례 원문이 비어있습니다.")

    config = config or GenerationConfig()
    cleaned = _clean_input(text)

    inputs = tokenizer(cleaned, return_tensors="pt", truncation=True, max_length=1024)
    # 토크나이저에 따라 token_type_ids 등 generate()가 못 받는 키가 섞여 나올 수 있어
    # (예: gogamza/kobart-base-v2), generate가 실제로 받는 키만 추린다.
    generate_inputs = {
        key: value for key, value in inputs.items() if key in ("input_ids", "attention_mask")
    }

    # model.to(device) 로 모델은 GPU에 있는데 입력 텐서는 CPU에 남아있으면
    # "Expected all tensors to be on the same device" 로 죽는다.
    # FakeModel(테스트용)처럼 .device 속성이 없는 경우도 있으니 있을 때만 옮긴다.
    model_device = getattr(model, "device", None)
    if model_device is not None:
        generate_inputs = {
            key: value.to(model_device) if hasattr(value, "to") else value
            for key, value in generate_inputs.items()
        }

    output_ids = model.generate(
        **generate_inputs,
        max_length=config.max_length,
        min_length=config.min_length,
        num_beams=config.num_beams,
        no_repeat_ngram_size=config.no_repeat_ngram_size,
        length_penalty=config.length_penalty,
        early_stopping=True,
    )
    summary = tokenizer.decode(output_ids[0], skip_special_tokens=True)
    return summary.strip()


# 법률 용어 -> 쉬운 풀이 매핑 (규칙 기반 예시 수준).
# 실제 서비스에서는 LLM 기반 재작성으로 교체하는 걸 권장하지만,
# KoBART 요약 결과를 감싸는 후처리 레이어만으로도 "일반인이 알아듣기 쉬운" 정도를
# 어느 정도 흉내낼 수 있어 최소 구현으로 넣어둔다.
_LEGAL_GLOSSARY: dict[str, str] = {
    "원고": "원고(소송을 제기한 사람)",
    "피고": "피고(소송을 당한 사람)",
    "상고": "상고(대법원에 다시 판단을 요청하는 절차)",
    "항소": "항소(1심 판결에 불복해 2심에 다시 판단을 요청하는 절차)",
    "기각": "기각(요청을 받아들이지 않음)",
    "파기환송": "파기환송(원심 판결을 취소하고 다시 재판하도록 돌려보냄)",
}


def explain_for_layperson(summary: str, glossary: dict[str, str] | None = None) -> str:
    """요약문에 등장하는 법률 용어에 쉬운 풀이를 덧붙인다.

    이미 풀이된 형태(예: "원고(소송을 제기한 사람)")가 포함돼 있으면 중복 치환하지 않는다.
    """
    glossary = glossary or _LEGAL_GLOSSARY
    result = summary
    for term, explained in glossary.items():
        if term in result and explained not in result:
            result = result.replace(term, explained)
    return result

"""
summarizer.py
=============
판례요약(KoBART) 모델을 서버 기동 시 한 번 로드해두고, main.py의
/summarize/precedent 엔드포인트에서 재사용하기 위한 얇은 래퍼.

precedent_summarizer 패키지(model.py)는 tokenizer/model을 인자로 주입받는
순수 함수 구조라서, 이 파일이 "FastAPI 라이프사이클에 맞춰 한 번만 로드하고
캐싱해두는" 부분만 담당한다.

환경변수:
  KOBART_CHECKPOINT : 학습된 체크포인트 디렉토리 경로
                       (예: "kobart/checkpoint-26606")
  KOBART_TOKENIZER  : 토크나이저 경로. 미지정 시 KOBART_CHECKPOINT와 동일 경로에서
                       찾는다 (거기 없으면 로드 실패 -> 로그에 원인 남고 서버는 계속 뜸,
                       /summarize/precedent만 503).
"""
import logging
import os

from precedent_summarizer.model import explain_for_layperson, load_summarizer, summarize

log = logging.getLogger("legal-chatbot")

KOBART_CHECKPOINT = os.environ.get("KOBART_CHECKPOINT", "kobart/checkpoint")
KOBART_TOKENIZER = os.environ.get("KOBART_TOKENIZER")  # None이면 model.py가 checkpoint 경로에서 찾음

_state: dict = {"tokenizer": None, "model": None}


def load_kobart_once() -> None:
    """서버 lifespan에서 한 번만 호출. 체크포인트가 없으면 경고만 남기고 조용히 스킵한다
    (챗봇 QA 어댑터가 없을 때와 동일한 정책 - 서버 전체가 죽지 않고 해당 기능만 503)."""
    if _state["model"] is not None:
        return
    if not os.path.isdir(KOBART_CHECKPOINT):
        log.warning(
            f"[kobart] 체크포인트 디렉토리가 없습니다: {KOBART_CHECKPOINT} "
            "-> /summarize/precedent 는 503을 반환합니다."
        )
        return

    log.info(f"[kobart] 체크포인트 로드 중: {KOBART_CHECKPOINT} (tokenizer={KOBART_TOKENIZER or '(checkpoint와 동일)'})")
    tokenizer, model = load_summarizer(KOBART_CHECKPOINT, tokenizer_path=KOBART_TOKENIZER)
    _state["tokenizer"] = tokenizer
    _state["model"] = model
    log.info("[kobart] 로드 완료.")


def is_ready() -> bool:
    return _state["model"] is not None


def summarize_precedent(text: str, plain: bool = True) -> tuple[str, str | None]:
    """판례 원문 -> (KoBART 원본 요약, 쉬운 풀이 버전(옵션))."""
    if not is_ready():
        raise RuntimeError("KoBART 모델이 로드되지 않았습니다. load_kobart_once()가 먼저 호출돼야 합니다.")

    raw_summary = summarize(text, _state["tokenizer"], _state["model"])
    plain_summary = explain_for_layperson(raw_summary) if plain else None
    return raw_summary, plain_summary

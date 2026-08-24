"""
법률 도메인 자동 분류 라우터
- 사용자가 법 지식 없이 올린 '날것' 질문을 받아 형사/민사/행정/지식재산 중
  관련된 도메인을(복수 가능) 점수와 함께 판별한다.
- 8B LLM과는 완전히 별개의 경량 모델(CPU)로 동작 -> GPU(VRAM)는 LLM 전용으로 남겨둔다.
- 방식: 다국어 NLI 기반 zero-shot 분류(mDeBERTa) + 한국어 법률 키워드 룰 하이브리드.
  라벨링된 학습 데이터 없이도 바로 쓸 수 있고, 나중에 실제 사용 로그가 쌓이면
  그 로그로 소형 분류기(KLUE-BERT 등)를 fine-tuning해서 교체하면 됨.
"""

import logging
from transformers import pipeline

log = logging.getLogger("legal-chatbot")

# 여러 언어를 지원하는 경량 NLI 모델 (약 280M, 8B LLM 대비 훨씬 가벼움)
ZS_MODEL_NAME = "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli"

# 라우터가 판별하는 도메인 키 <-> 분류기에 넣을 한국어 라벨
DOMAIN_LABELS = {
    "criminal": "형사",
    "civil": "민사",
    "administrative": "행정",
    "intellectual_property": "지식재산",
}

HYPOTHESIS_TEMPLATE = "이 질문은 {} 법에 관한 것이다."

# ──────────────────────────────────────────────────────────────
# 법률 질문 여부 게이트 (도메인 분류 이전 단계)
# ──────────────────────────────────────────────────────────────
# classify_domains()의 도메인별 점수는 "형사법에 관한 것이다/아니다"를 각 도메인마다
# 독립적으로(multi_label=True) 판단하는 방식이라, "법률 질문 자체가 맞는지"를 직접
# 비교할 절대 기준이 없다. 그래서 코드 작성 요청, 잡담처럼 법률과 무관한 입력도
# 어느 도메인에 대해 명확히 "아니다"라고 부정할 단서가 없으면 애매한 점수(0.4~0.6대)가
# 나오고, 그게 threshold를 넘어 억지로 어떤 도메인이든 골라잡히는 문제가 있었다.
#
# 아래 게이트는 "법률 상담 관련 질문" vs "법률과 무관한 일반적인 대화/요청" 두 라벨을
# multi_label=False(서로 확률을 나눠 가짐, 합계=1)로 직접 경쟁시켜서 상대적으로 비교한다.

GATE_LABELS = ["법률 상담과 관련된 질문", "법률과 무관한 일반적인 대화나 요청"]
GATE_HYPOTHESIS_TEMPLATE = "이 문장은 {}이다."
GATE_MIN_CONFIDENCE = 0.6  # 이 값 미만이면 애매한 것으로 보고 보수적으로 "법률 질문 아님" 처리

# 모델 호출 없이 즉시 걸러낼 수 있는 명백한 케이스들 (오탐 위험이 낮은 것만).
# 여기 걸리면 게이트 모델조차 호출하지 않고 바로 "법률 질문 아님"으로 판정한다.
_NON_LEGAL_HINTS = [
    # 프로그래밍/코드 요청
    "코드를 짜", "코드 짜", "코드짜줘", "코드 작성", "프로그램을 작성", "함수를 작성",
    "python", "파이썬 코드", "javascript", "자바스크립트 코드", "java 코드", "c++",
    "html코드", "css코드", " sql ", "알고리즘을 짜",
    # 흔한 잡담/인사
    "안녕하세요", "안녕", "반가워", "심심해", "너 누구야", "너는 누구",
]

# 아주 흔한 비속어 어근 몇 개만 최소한으로 필터링 (완전한 욕설 탐지기가 아니라,
# 명백히 법률 상담이 아닌 것으로 보이는 케이스를 빠르게 쳐내는 용도).
_PROFANITY_HINTS = ["시발", "씨발", "개새끼", "병신"]


def _quick_non_legal_check(text: str) -> bool:
    """True면 모델 호출 없이 바로 '법률 질문 아님'으로 판정."""
    lowered = text.lower()
    if any(hint.lower() in lowered for hint in _NON_LEGAL_HINTS):
        return True
    if any(hint in text for hint in _PROFANITY_HINTS):
        return True
    return False


def is_legal_question(text: str, min_confidence: float = GATE_MIN_CONFIDENCE) -> tuple[bool, float]:
    """(법률 질문으로 판단되는지, 게이트 신뢰도 점수)를 반환.

    - 명백한 비법률 패턴(코드 요청, 인사말, 비속어)은 모델 호출 없이 바로 False 처리
    - 그 외에는 2지선다 zero-shot으로 "법률 질문" vs "일반 대화" 신뢰도를 비교
    - 신뢰도가 min_confidence 미만이면(=애매하면) 보수적으로 법률 질문이 아닌 것으로 처리
      (챗봇 성격상 '법률 질문을 놓치는 것'보다 '엉뚱한 질문에 그럴듯하게 답하는 것'이
      더 위험하다고 보고 보수적으로 설계함. 필요하면 min_confidence를 낮춰서 완화 가능)
    """
    if not text or not text.strip():
        return False, 1.0

    if _quick_non_legal_check(text):
        return False, 1.0

    zs = _get_pipeline()
    result = zs(
        text,
        candidate_labels=GATE_LABELS,
        hypothesis_template=GATE_HYPOTHESIS_TEMPLATE,
        multi_label=False,
    )
    top_label = result["labels"][0]
    top_score = result["scores"][0]

    is_legal = (top_label == GATE_LABELS[0]) and top_score >= min_confidence
    return is_legal, round(top_score, 4)

# 보조 신호로 쓰는 키워드 룰. zero-shot 모델이 애매하게 판단할 때 점수를 보정해줌.
DOMAIN_KEYWORDS = {
    "criminal": [
        "고소", "고발", "형사", "구속", "기소", "불기소", "벌금", "징역", "폭행",
        "절도", "사기죄", "음주운전", "수사", "경찰조사", "합의", "집행유예",
        "정당방위", "성범죄", "명예훼손", "무고", "체포", "구금", "전과",
    ],
    "civil": [
        "손해배상", "계약", "임대차", "전세", "보증금", "이혼", "위자료", "상속",
        "채권", "채무", "대여금", "매매", "부동산", "임금체불", "민사소송",
        "하자보수", "위약금", "협의이혼", "친권", "양육권", "지급명령",
    ],
    "administrative": [
        "행정심판", "행정소송", "인허가", "영업정지", "과태료", "행정처분",
        "공무원", "면허취소", "국가배상", "이의신청", "건축허가", "산업재해",
        "국민연금", "보조금", "인가", "허가취소", "행정지도",
    ],
    "intellectual_property": [
        "특허", "상표", "저작권", "디자인권", "실용신안", "표절", "지식재산권",
        "라이선스", "침해금지", "영업비밀", "저작인접권", "상표등록", "특허출원",
        "카피", "무단도용",
    ],
}

_zs_pipeline = None


def _get_pipeline():
    """zero-shot 분류 파이프라인을 최초 호출 시 1회만 로드 (lazy singleton)."""
    global _zs_pipeline
    if _zs_pipeline is None:
        log.info(f"경량 분류 모델 로드 중: {ZS_MODEL_NAME} (CPU)")
        _zs_pipeline = pipeline(
            "zero-shot-classification",
            model=ZS_MODEL_NAME,
            device=-1,  # CPU 고정. GPU(VRAM)는 8B LLM 전용으로 비워둔다.
        )
        log.info("경량 분류 모델 로드 완료.")
    return _zs_pipeline


def _keyword_scores(text: str) -> dict:
    scores = {}
    for domain, kws in DOMAIN_KEYWORDS.items():
        hits = sum(1 for kw in kws if kw in text)
        scores[domain] = min(hits / 3, 1.0)  # 키워드 3개 이상 매칭되면 만점 취급
    return scores


def classify_domains(text: str, threshold: float = 0.35, keyword_weight: float = 0.3):
    """
    질문 텍스트 -> 관련 법 도메인(들)을 점수와 함께 반환.

    반환값: [(domain_key, score), ...]  점수 내림차순, threshold 이상만 포함.
            단, 아무 도메인도 threshold를 못 넘으면(=매우 애매한 질문)
            최고 점수 도메인 1개는 강제로 포함시켜 '무응답'을 방지한다.
    """
    if not text or not text.strip():
        return [("criminal", 0.0)]  # 빈 입력 방어 (호출부에서 별도 검증 권장)

    zs = _get_pipeline()
    labels = list(DOMAIN_LABELS.values())
    label_to_key = {v: k for k, v in DOMAIN_LABELS.items()}

    result = zs(
        text,
        candidate_labels=labels,
        hypothesis_template=HYPOTHESIS_TEMPLATE,
        multi_label=True,
    )
    zs_scores = {label_to_key[label]: score for label, score in zip(result["labels"], result["scores"])}
    kw_scores = _keyword_scores(text)

    combined = {}
    for domain in DOMAIN_LABELS:
        combined[domain] = (
            (1 - keyword_weight) * zs_scores.get(domain, 0.0)
            + keyword_weight * kw_scores.get(domain, 0.0)
        )

    ranked = sorted(combined.items(), key=lambda x: x[1], reverse=True)
    detected = [(d, s) for d, s in ranked if s >= threshold]

    if not detected:
        detected = [ranked[0]]

    return detected

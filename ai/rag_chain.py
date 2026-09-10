"""
rag_chain.py
============
main.py가 임포트하는 RAG 연결 모듈.
db_loader.py로 만든 FAISS(Dense) + BM25(Keyword) 인덱스를 로딩해두고,
질문이 들어오면 하이브리드(RRF) 검색 -> 판례/법조문 중복 제거 -> SourceDoc 형식 변환 -> 근거주의 프롬프트 메시지
구성까지 담당한다.
"""

from __future__ import annotations

import logging
import os
from pathlib import Path

from db_loader import load_indexes, LegalType

logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger("legal-chatbot")

ALL_LEGAL_TYPES: list[str] = ["civil", "criminal", "administrative"]
_INDEX_CACHE: dict[str, tuple] = {}
_RRF_K = 60
# .env(RAG_SCORE_THRESHOLD)로 재배포 없이 튜닝할 수 있게 함. 기본값은 기존과 동일한 1.0.
# (criminal 도메인에서 사실관계가 다른 판례가 threshold를 넘어 들어오는 문제가 확인됐는데,
# 정확히 얼마나 당겨야 하는지는 실제 점수 분포를 봐야 알 수 있어서 코드에 고정값을 박지 않음
# - RAG_DEBUG=true로 실제 점수 확인 후 .env에서 값을 조정하면 됨.)
_DENSE_SCORE_THRESHOLD = float(os.environ.get("RAG_SCORE_THRESHOLD", "1.0"))

# 검색 후보 전체(점수 + 내용)를 로그로 보고 싶을 때만 켠다.
# RAG_DEBUG=true 로 서버를 띄우면 retrieve_context()가 호출될 때마다 dense 검색
# 상위 후보들을 (점수, 내용 앞부분)까지 로그로 남긴다. 기본값은 false라 평소엔
# 로그가 늘어나지 않음 - "필요할 때만 코드 수정 없이 켠다"는 목적.
# 내용은 200자까지만 잘라서 보여주는데, 그래도 부족하면 debug_criminal_qa.py 처럼
# _content를 통째로 출력하는 스크립트를 쓰는 게 낫다 (로그가 너무 길어지지 않게).
RAG_DEBUG = os.environ.get("RAG_DEBUG", "false").lower() == "true"
_DEBUG_PREVIEW_CHARS = 200

# ──────────────────────────────────────────────────────────────
# Spring 백엔드(LegalSourceResponse: lawName/articleNumber/url) 연동용.
# db_loader.py가 이미 metadata에 source_id(법령ID또는 판례일련번호)를 채워두므로,
# 그걸로 국가법령정보센터 상세 페이지 URL을 만들어 응답에 실어보낸다.
#
# source_id는 db_loader.py에서 법령=법령일련번호(MST), 판례=판례일련번호로 채워진다.
# (법령ID를 넣으면 엉뚱한 법령이 열리므로 반드시 MST여야 함 — 2024-xx-xx 버그 수정)
# 실제로 브라우저에서 열어서 검증 완료:
#   - https://www.law.go.kr/LSW/lsInfoP.do?lsiSeq=<MST> → 해당 법령 본문
#   - https://www.law.go.kr/precInfoP.do?precSeq=<판례일련번호> → 해당 판례 본문
#   - joNo(조번호 4자리)/joBrNo(조가지번호 2자리)를 추가하면 특정 조문으로 바로 이동 가능
# ──────────────────────────────────────────────────────────────
_LAW_DETAIL_URL = "https://www.law.go.kr/LSW/lsInfoP.do?lsiSeq={source_id}"
_PREC_DETAIL_URL = "https://www.law.go.kr/precInfoP.do?precSeq={source_id}"


def build_source_url(docu_type: str, source_id: str, jo_no: str = "", jo_br_no: str = "") -> str:
    if not source_id:
        return ""
    if docu_type == "법령":
        url = _LAW_DETAIL_URL.format(source_id=source_id)
        if jo_no:
            url += f"&joNo={jo_no}&joBrNo={jo_br_no or '00'}"
        return url
    if docu_type in ("판례", "해석례", "결정례"):
        return _PREC_DETAIL_URL.format(source_id=source_id)
    return ""


def _index_dir_exists(legal_type: str) -> bool:
    return Path("./indexes") / legal_type / "faiss" / "index.faiss"


def _get_index(legal_type: str):
    if legal_type not in _INDEX_CACHE:
        index_path = Path("./indexes") / legal_type / "faiss" / "index.faiss"
        if not index_path.exists():
            raise FileNotFoundError(
                f"'{legal_type}' 인덱스가 없습니다 ({index_path}). "
                f"db_loader.py --legal-type {legal_type} 를 먼저 실행하세요."
            )
        logger.info(f"[{legal_type}] 인덱스 로딩 중...")
        _INDEX_CACHE[legal_type] = load_indexes(legal_type)  # type: ignore[arg-type]
        logger.info(f"[{legal_type}] 인덱스 로딩 완료.")
    return _INDEX_CACHE[legal_type]


def preload_all_retrievers(legal_types: list[str] | None = None) -> None:
    legal_types = legal_types or ALL_LEGAL_TYPES
    for lt in legal_types:
        try:
            _get_index(lt)
        except FileNotFoundError as e:
            logger.warning(f"[{lt}] 인덱스 미리 로딩 건너뜀: {e}")
        except Exception:
            logger.exception(f"[{lt}] 인덱스 로딩 중 예상치 못한 오류")


def _rrf_fuse(dense_docs: list, sparse_docs: list, top_n: int) -> list:
    scores: dict[str, float] = {}
    doc_map: dict[str, object] = {}

    for rank, doc in enumerate(dense_docs):
        key = doc.page_content
        scores[key] = scores.get(key, 0.0) + 1.0 / (_RRF_K + rank + 1)
        doc_map[key] = doc

    for rank, doc in enumerate(sparse_docs):
        key = doc.page_content
        scores[key] = scores.get(key, 0.0) + 1.0 / (_RRF_K + rank + 1)
        doc_map[key] = doc

    ranked_keys = sorted(scores.items(), key=lambda kv: kv[1], reverse=True)
    return [doc_map[key] for key, _ in ranked_keys[:top_n]]


def retrieve_context(
    legal_type: str, question: str, k: int = 5, score_threshold: float = _DENSE_SCORE_THRESHOLD
) -> list[dict]:
    faiss_index, bm25_retriever = _get_index(legal_type)

    dense_pool = max(k * 4, 20)
    dense_hits = faiss_index.similarity_search_with_score(question, k=dense_pool)
    dense_docs = [d for d, score in dense_hits if score <= score_threshold]
    # RRF 융합 단계에서 원래 dense 점수(거리)가 사라지므로, page_content 기준으로 따로 보관해뒀다가
    # 최종 결과에 다시 붙인다 (RAG_DEBUG 없이도 score를 보고 threshold를 튜닝할 수 있게).
    dense_score_map = {doc.page_content: score for doc, score in dense_hits}

    if RAG_DEBUG:
        logger.info(f"[{legal_type}] RAG_DEBUG 쿼리: {question!r}")
        for rank, (doc, score) in enumerate(dense_hits, start=1):
            meta = doc.metadata
            label = " ".join(
                p for p in [meta.get("law_name"), meta.get("article_no"), meta.get("case_num")] if p
            ) or meta.get("docu_type", "출처 미상")
            kept = "유지" if score <= score_threshold else "제외(threshold 초과)"
            preview = doc.page_content[:_DEBUG_PREVIEW_CHARS].replace("\n", " ")
            logger.info(f"[{legal_type}] RAG_DEBUG #{rank} score={score:.4f} ({kept}) {label} | {preview}")

    dropped = len(dense_hits) - len(dense_docs)
    if dropped:
        logger.info(
            f"[{legal_type}] dense 검색 {len(dense_hits)}건 중 {dropped}건을 "
            f"score_threshold={score_threshold} 초과로 제외"
        )

    # (2026-09: threshold 완화 fallback 제거)
    # 예전엔 근거 문서가 _MIN_DENSE_DOCS(2건) 미만이면 threshold를 완화해 재필터링했는데,
    # 이게 "양자과학기술법"처럼 질문과 무관한 문서를 억지로 끌어와 답변에 인용되는 원인이었다.
    # format_context_block()이 이미 근거가 없을 때 "구체적 조문·판례를 단정 인용하지 말라"는
    # 안전한 경로를 모델에게 제공하므로, 무관한 문서로 채우는 것보다 그냥 근거 없음으로
    # 남겨두는 편이 안전하다. 근거 문서 수가 적을 때는 로그만 남긴다.
    if len(dense_docs) < 2:
        logger.info(
            f"[{legal_type}] 근거 문서 부족({len(dense_docs)}건) -> "
            f"threshold 완화 없이 그대로 진행 (근거 없음 안내 경로 사용)"
        )

    bm25_retriever.k = dense_pool
    sparse_docs = bm25_retriever.invoke(question)

    dense_contents = {doc.page_content for doc in dense_docs}
    sparse_docs = [doc for doc in sparse_docs if doc.page_content in dense_contents]

    fused_docs = _rrf_fuse(dense_docs, sparse_docs, top_n=len(dense_contents))

    # [핵심] 동일한 판례나 동일한 조문으로 인한 도배 방지 (Deduplication)
    unique_fused_docs = []
    seen_identifiers = set()

    for doc in fused_docs:
        meta = doc.metadata
        case_num = (meta.get("case_num") or "").strip()
        law_name = (meta.get("law_name") or "").strip()
        article_no = (meta.get("article_no") or "").strip()

        if case_num:
            doc_id = f"case_{case_num}"
        elif law_name and article_no:
            doc_id = f"law_{law_name}_{article_no}"
        else:
            doc_id = doc.page_content[:50]

        if doc_id not in seen_identifiers:
            unique_fused_docs.append(doc)
            seen_identifiers.add(doc_id)

        if len(unique_fused_docs) >= k:
            break

    results: list[dict] = []
    for rank, doc in enumerate(unique_fused_docs, start=1):
        meta = doc.metadata
        docu_type = meta.get("docu_type", "") or ""
        source_id = meta.get("source_id", "") or ""
        jo_no = meta.get("jo_no", "") or ""
        jo_br_no = meta.get("jo_br_no", "") or ""
        results.append({
            "rank": rank,
            "law_name": meta.get("law_name", "") or "",
            "article_no": meta.get("article_no", "") or "",
            "docu_type": docu_type,
            "case_num": meta.get("case_num", "") or "",
            "url": build_source_url(docu_type, source_id, jo_no, jo_br_no),
            "_content": doc.page_content,
            # BM25 단독으로 걸려 dense_score_map에 없는 문서는 None (SourceDoc(**s)는 extra
            # 필드를 무시하므로 main.py의 응답 스키마엔 영향 없음 - 디버깅/튜닝 용도로만 씀).
            "_dense_score": dense_score_map.get(doc.page_content),
        })
    return results


def format_context_block(retrieved: list[dict]) -> str:
    if not retrieved:
        return "(관련된 법령/판례 검색 결과가 없습니다. 일반적인 법률 지식으로 답변하되, " \
               "구체적인 조문·판례 번호를 단정적으로 인용하지 마시오.)"

    lines = []
    for item in retrieved:
        label_parts = [p for p in [item.get("law_name"), item.get("article_no"), item.get("case_num")] if p]
        label = " ".join(label_parts) or item.get("docu_type", "출처 미상")
        lines.append(f"[{item['rank']}] {label}\n{item['_content']}")
    return "\n\n".join(lines)


def build_rag_messages(instruction: str, question: str, context_block: str) -> list[dict]:
    system_msg = (
        "당신은 대한민국 법률 전문 AI 어시스턴트입니다. "
        "아래 제공된 [참고 자료]에 근거하여 정확하고 문맥에 맞는 자연스러운 한국어로 답변하십시오.\n\n"
        "반드시 지킬 규칙:\n"
        "1. [참고 자료]의 법리와 사실관계를 왜곡하지 마십시오.\n"
        "2. 알 수 없는 단어나 비정상적인 어휘를 만들어내지 마십시오.\n"
        "3. 문장은 완전한 문장으로 명확하게 끝맺으십시오.\n"
        "4. 자료에 없는 내용은 단정하여 서술하지 마십시오.\n"
        "5. [참고 자료]에 담긴 사건의 당사자 관계나 사실관계(예: 가해자/피해자 구도, 사건의 종류)가 "
        "질문 속 상황과 명백히 다르면, 그 자료의 구체적 내용(사건번호·세부 사실)을 인용하지 말고 "
        "관련 법령의 일반적인 법리만 설명하십시오.\n\n"
    )
    user_message = (
        f"지시 : {instruction}\n\n"
        f"[참고 자료]\n{context_block}\n\n"
        f'질문 : "{question}"\n'
    )
    return [
        {"role": "system", "content": system_msg},
        {"role": "user", "content": f"{user_message}\n\n"},
    ]
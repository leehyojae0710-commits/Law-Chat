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
from pathlib import Path

from db_loader import load_indexes, LegalType

logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger("legal-chatbot")

ALL_LEGAL_TYPES: list[str] = ["civil", "criminal", "administrative"]
_INDEX_CACHE: dict[str, tuple] = {}
_RRF_K = 60
_DENSE_SCORE_THRESHOLD = 1.0

# ──────────────────────────────────────────────────────────────
# Spring 백엔드(LegalSourceResponse: lawName/articleNumber/url) 연동용.
# db_loader.py가 이미 metadata에 source_id(법령ID 또는 판례일련번호)를 채워두므로,
# 그걸로 국가법령정보센터 상세 페이지 URL을 만들어 응답에 실어보낸다.
#
# ⚠️ 주의: 아래 URL 패턴은 law.go.kr의 공개적으로 알려진 상세페이지 규칙을 기반으로
# 작성한 것으로, 실제 응답 원본 문서에서 눈으로 직접 한 번 검증 후 쓰는 걸 권장합니다.
# (법령/판례 각각 실제 링크가 열리는지 브라우저로 확인해보세요.)
# ──────────────────────────────────────────────────────────────
_LAW_DETAIL_URL = "https://www.law.go.kr/LSW/lsInfoP.do?lsiSeq={source_id}"
_PREC_DETAIL_URL = "https://www.law.go.kr/precInfoP.do?precSeq={source_id}"


def build_source_url(docu_type: str, source_id: str) -> str:
    if not source_id:
        return ""
    if docu_type == "법령":
        return _LAW_DETAIL_URL.format(source_id=source_id)
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

    dropped = len(dense_hits) - len(dense_docs)
    if dropped:
        logger.info(
            f"[{legal_type}] dense 검색 {len(dense_hits)}건 중 {dropped}건을 "
            f"score_threshold={score_threshold} 초과로 제외"
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
        results.append({
            "rank": rank,
            "law_name": meta.get("law_name", "") or "",
            "article_no": meta.get("article_no", "") or "",
            "docu_type": docu_type,
            "case_num": meta.get("case_num", "") or "",
            "url": build_source_url(docu_type, source_id),
            "_content": doc.page_content,
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
        "4. 자료에 없는 내용은 단정하여 서술하지 마십시오.\n\n"
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
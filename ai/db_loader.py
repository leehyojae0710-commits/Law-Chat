"""
db_loader.py
============
국가법령정보센터 Open API(OC 키)로 법률 데이터를 실시간 수집하고,
조문·문단 단위로 청킹한 뒤 FAISS(Dense) + BM25(Keyword) 인덱스를 만들어
로컬에 저장하는 모듈. CSV 업로드 없이 API 키만으로 전체 파이프라인이 돈다.

법제처 Open API는 카테고리별로 target 파라미터가 다르다 (2026-08 기준 확인):
    법령         -> target=law
    판례(판결문) -> target=prec
    법령해석례   -> target=expc
    헌재결정례   -> target=detc

각 target마다
    1) lawSearch.do  : 키워드로 검색해서 목록(일련번호 등)을 받고
    2) lawService.do : 그 일련번호(ID)로 본문 전체를 받는다
2단계 흐름은 4개 카테고리 모두 동일하다.
"""

from __future__ import annotations

import os
import re
import json
import time
import logging
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Literal

import requests
from lxml import etree
from tqdm import tqdm
from dotenv import load_dotenv

from langchain_community.vectorstores import FAISS
from langchain_community.retrievers import BM25Retriever
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document

load_dotenv()

logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

LegalType = Literal["civil", "criminal", "administrative", "ip"]
DocuType = Literal["법령", "판례", "해석례", "결정례"]

LAW_API_BASE = "http://www.law.go.kr/DRF"

# 카테고리(한글) <-> API target 파라미터 매핑
TARGET_MAP: dict[DocuType, str] = {
    "법령": "law",
    "판례": "prec",
    "해석례": "expc",
    "결정례": "detc",
}


# ──────────────────────────────────────────────────────────────
# 0. 공통 API 호출 유틸
# ──────────────────────────────────────────────────────────────

class LawAPIError(RuntimeError):
    pass


def _get_oc() -> str:
    oc = os.getenv("LAW_API_OC")
    if not oc:
        raise LawAPIError(
            "LAW_API_OC 환경변수가 없습니다. .env 파일에 국가법령정보센터에서 "
            "발급받은 OC 값을 넣어주세요 (보통 가입 이메일의 '@' 앞부분)."
        )
    return oc


def _http_get_with_retry(url: str, params: dict, timeout: int = 15,
                         max_retries: int = 4, backoff_base: float = 2.0) -> requests.Response:
    """일시적 네트워크 장애(DNS 실패, 타임아웃, 커넥션 오류 등)에 대비한 재시도 래퍼."""
    last_exc: Exception | None = None
    for attempt in range(1, max_retries + 1):
        try:
            resp = requests.get(url, params=params, timeout=timeout)
            resp.raise_for_status()
            return resp
        except requests.exceptions.RequestException as e:
            last_exc = e
            if attempt < max_retries:
                wait = backoff_base * (2 ** (attempt - 1))
                logger.warning(f"API 호출 실패({attempt}/{max_retries}), {wait:.0f}초 후 재시도: {e}")
                time.sleep(wait)
            else:
                logger.error(f"API 호출 최종 실패({max_retries}회 시도): {e}")
    raise last_exc  # type: ignore[misc]


def _api_search(target: str, query: str, display: int = 20, **extra_params) -> etree._Element:
    """lawSearch.do 호출 -> 목록(XML root) 반환."""
    params = {"OC": _get_oc(), "target": target, "type": "XML", "query": query, "display": display}
    params.update(extra_params)
    resp = _http_get_with_retry(f"{LAW_API_BASE}/lawSearch.do", params=params, timeout=15)
    return etree.fromstring(resp.content)


def _api_service(target: str, doc_id: str, **extra_params) -> etree._Element:
    """lawService.do 호출 -> 본문(XML root) 반환."""
    params = {"OC": _get_oc(), "target": target, "type": "XML", "ID": doc_id}
    params.update(extra_params)
    resp = _http_get_with_retry(f"{LAW_API_BASE}/lawService.do", params=params, timeout=15)
    return etree.fromstring(resp.content)


def _xtext(node, *candidate_tags: str) -> str:
    """후보 태그 이름들을 순서대로 시도해서 첫 매치 텍스트를 반환."""
    for tag in candidate_tags:
        found = node.find(f".//{tag}")
        if found is not None and found.text:
            text = found.text
            text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
            text = re.sub(r"<[^>]+>", "", text)
            text = re.sub(r"\n{3,}", "\n\n", text)
            return text.strip()
    return ""


def debug_raw_response(target: str, query: str) -> None:
    """실제 API 응답 XML을 그대로 출력."""
    root = _api_search(target, query, display=1)
    print(etree.tostring(root, pretty_print=True).decode("utf-8"))


# ──────────────────────────────────────────────────────────────
# 1. 공통 데이터 구조
# ──────────────────────────────────────────────────────────────

@dataclass
class LegalDocument:
    content: str
    law_name: str
    article_no: str = ""
    docu_type: str = "법령"
    legal_type: str = "civil"
    case_num: str = ""
    source_id: str = ""
    extra: dict = field(default_factory=dict)

    def to_langchain_document(self) -> Document:
        return Document(
            page_content=self.content,
            metadata={
                "law_name": self.law_name,
                "article_no": self.article_no,
                "docu_type": self.docu_type,
                "legal_type": self.legal_type,
                "case_num": self.case_num,
                "source_id": self.source_id,
                **self.extra,
            },
        )


# ──────────────────────────────────────────────────────────────
# 2. 법령 (target=law)
# ──────────────────────────────────────────────────────────────

def search_statute(query: str, display: int = 20) -> list[dict]:
    root = _api_search("law", query, display=display)
    return [
        {
            "법령명": _xtext(law, "법령명한글"),
            "법령ID": _xtext(law, "법령ID"),
            "MST": _xtext(law, "법령일련번호"),
        }
        for law in root.findall(".//law")
    ]


def fetch_statute_from_api(law_id: str, legal_type: LegalType, mst: str | None = None) -> list[LegalDocument]:
    """법령 본문을 조/항/호/목 단위로 청킹."""
    root = _api_service("law", law_id, **({"MST": mst} if mst else {}))
    law_name = _xtext(root, "법령명_한글", "법령명한글")

    # ⚠️ law.go.kr 상세페이지(lsInfoP.do)의 lsiSeq 파라미터는 "법령ID"가 아니라
    # "법령일련번호(MST)"를 요구한다. law_id를 그대로 넣으면 전혀 다른 법령이 열릴 수 있으므로
    # source_id는 반드시 mst를 사용한다 (mst가 없으면 URL을 만들 수 없으니 빈 값으로 둔다).
    url_source_id = mst or ""

    docs: list[LegalDocument] = []
    for jo in root.findall(".//조문단위"):
        jo_no = _xtext(jo, "조문번호")
        jo_gaji = _xtext(jo, "조문가지번호")
        jo_title = _xtext(jo, "조문제목")
        jo_content = _xtext(jo, "조문내용")
        article_label = f"제{jo_no}조" + (f"의{jo_gaji}" if jo_gaji and jo_gaji != "0" else "")

        # joNo/joBrNo 딥링크 파라미터 (law.go.kr은 조번호 4자리, 조가지번호 2자리로 0-padding)
        jo_link_extra = {}
        if jo_no:
            jo_link_extra = {
                "jo_no": jo_no.zfill(4),
                "jo_br_no": (jo_gaji or "0").zfill(2),
            }

        if jo_content.strip():
            docs.append(LegalDocument(
                content=f"{article_label}({jo_title}) {jo_content}".strip(),
                law_name=law_name, article_no=article_label,
                docu_type="법령", legal_type=legal_type, source_id=url_source_id,
                extra=dict(jo_link_extra),
            ))
        for hang in jo.findall(".//항"):
            hang_no = _xtext(hang, "항번호")
            hang_content = _xtext(hang, "항내용")
            hang_label = f"{article_label}{hang_no}".strip()

            if hang_content.strip():
                docs.append(LegalDocument(
                    content=f"{hang_label} {hang_content}".strip(),
                    law_name=law_name, article_no=article_label,
                    docu_type="법령", legal_type=legal_type, source_id=url_source_id,
                    extra={"chunk_level": "항", **jo_link_extra},
                ))

            for ho in hang.findall("./호"):
                ho_no = _xtext(ho, "호번호")
                ho_content = _xtext(ho, "호내용")
                ho_label = f"{hang_label}{ho_no}".strip()

                if ho_content.strip():
                    docs.append(LegalDocument(
                        content=f"{ho_label} {ho_content}".strip(),
                        law_name=law_name, article_no=article_label,
                        docu_type="법령", legal_type=legal_type, source_id=url_source_id,
                        extra={"chunk_level": "호", **jo_link_extra},
                    ))

                for mok in ho.findall("./목"):
                    mok_no = _xtext(mok, "목번호")
                    mok_content = _xtext(mok, "목내용")
                    if mok_content.strip():
                        docs.append(LegalDocument(
                            content=f"{ho_label}{mok_no} {mok_content}".strip(),
                            law_name=law_name, article_no=article_label,
                            docu_type="법령", legal_type=legal_type, source_id=url_source_id,
                            extra={"chunk_level": "목", **jo_link_extra},
                        ))
    logger.info(f"[법령] {law_name} (법령ID={law_id}, MST={mst}) → {len(docs)}개 청크")
    return docs


# ──────────────────────────────────────────────────────────────
# 3. 판례 (target=prec)
# ──────────────────────────────────────────────────────────────

def search_precedent(query: str, display: int = 20) -> list[dict]:
    root = _api_search("prec", query, display=display)
    return [
        {
            "판례일련번호": _xtext(p, "판례일련번호"),
            "사건명": _xtext(p, "사건명"),
            "사건번호": _xtext(p, "사건번호"),
            "법원명": _xtext(p, "법원명"),
            "선고일자": _xtext(p, "선고일자"),
        }
        for p in root.findall(".//prec")
    ]


def fetch_precedent_from_api(prec_id: str, legal_type: LegalType) -> list[LegalDocument]:
    """판례 본문을 판시사항/판결요지/전체 판례내용 단위로 청킹."""
    root = _api_service("prec", prec_id)
    case_name = _xtext(root, "사건명")
    case_num = _xtext(root, "사건번호")

    docs: list[LegalDocument] = []
    for section_tag, section_label in [("판시사항", "판시사항"), ("판결요지", "판결요지")]:
        text = _xtext(root, section_tag)
        if text:
            docs.append(LegalDocument(
                content=text, law_name="", docu_type="판례", legal_type=legal_type,
                case_num=case_num, source_id=prec_id, extra={"section": section_label, "case_name": case_name},
            ))
    full_text = _xtext(root, "판례내용")
    if full_text:
        for i, chunk in enumerate(_split_long_text(full_text)):
            docs.append(LegalDocument(
                content=chunk, law_name="", docu_type="판례", legal_type=legal_type,
                case_num=case_num, source_id=prec_id,
                extra={"section": f"판례내용_{i}", "case_name": case_name},
            ))
    logger.info(f"[판례] {case_name} ({case_num}) → {len(docs)}개 청크")
    return docs


# ──────────────────────────────────────────────────────────────
# 4. 법령해석례 (target=expc)
# ──────────────────────────────────────────────────────────────

def search_interpretation(query: str, display: int = 20) -> list[dict]:
    root = _api_search("expc", query, display=display)
    return [
        {
            "법령해석례일련번호": _xtext(e, "법령해석례일련번호", "안건일련번호"),
            "안건명": _xtext(e, "안건명"),
            "안건번호": _xtext(e, "안건번호"),
            "해석기관명": _xtext(e, "회신기관명", "질의기관명"),
            "회신일자": _xtext(e, "회신일자"),
        }
        for e in root.findall(".//expc")
    ]


def fetch_interpretation_from_api(interp_id: str, legal_type: LegalType) -> list[LegalDocument]:
    root = _api_service("expc", interp_id)
    agenda = _xtext(root, "안건명")
    agenda_num = _xtext(root, "안건번호")

    docs: list[LegalDocument] = []
    for section_tag, section_label in [("질의요지", "질의요지"), ("회답", "회답"), ("이유", "이유")]:
        text = _xtext(root, section_tag)
        if text:
            for i, chunk in enumerate(_split_long_text(text)):
                docs.append(LegalDocument(
                    content=chunk, law_name="", docu_type="해석례", legal_type=legal_type,
                    case_num=agenda_num, source_id=interp_id,
                    extra={"section": f"{section_label}_{i}", "case_name": agenda},
                ))
    logger.info(f"[해석례] {agenda} ({agenda_num}) → {len(docs)}개 청크")
    return docs


# ──────────────────────────────────────────────────────────────
# 5. 헌재결정례 (target=detc)
# ──────────────────────────────────────────────────────────────

def search_decision(query: str, display: int = 20) -> list[dict]:
    root = _api_search("detc", query, display=display)
    return [
        {
            "헌재결정례일련번호": _xtext(d, "헌재결정례일련번호"),
            "사건명": _xtext(d, "사건명"),
            "사건번호": _xtext(d, "사건번호"),
            "종국일자": _xtext(d, "종국일자"),
        }
        for d in root.findall(".//detc")
    ]


def fetch_decision_from_api(detc_id: str, legal_type: LegalType) -> list[LegalDocument]:
    root = _api_service("detc", detc_id)
    case_name = _xtext(root, "사건명")
    case_num = _xtext(root, "사건번호")

    docs: list[LegalDocument] = []
    for section_tag, section_label in [("판시사항", "판시사항"), ("결정요지", "결정요지"), ("결정문", "결정문")]:
        text = _xtext(root, section_tag)
        if text:
            for i, chunk in enumerate(_split_long_text(text)):
                docs.append(LegalDocument(
                    content=chunk, law_name="", docu_type="결정례", legal_type=legal_type,
                    case_num=case_num, source_id=detc_id,
                    extra={"section": f"{section_label}_{i}", "case_name": case_name},
                ))
    logger.info(f"[결정례] {case_name} ({case_num}) → {len(docs)}개 청크")
    return docs


# ──────────────────────────────────────────────────────────────
# 6. 공통 헬퍼 - 긴 본문 분할
# ──────────────────────────────────────────────────────────────

def _split_long_text(text: str, max_chars: int = 500, overlap: int = 50) -> list[str]:
    """문단이 너무 길면 임베딩/검색 품질을 위해 겹치게 슬라이딩 분할."""
    text = text.strip()
    if len(text) <= max_chars:
        return [text]
    chunks = []
    start = 0
    while start < len(text):
        end = start + max_chars
        chunks.append(text[start:end])
        start = end - overlap
    return chunks


# ──────────────────────────────────────────────────────────────
# 7. legal_type별 수집 - 검색 키워드 목록을 받아 4개 카테고리 전부 수집
# ──────────────────────────────────────────────────────────────

DEFAULT_KEYWORDS: dict[LegalType, list[str]] = {
    "civil": ["민법", "임대차", "손해배상", "채권", "상속", "이혼"],
    "criminal": ["형법", "형사소송법", "폭행", "절도", "사기", "살인", "강도", "성폭력"],
    "administrative": ["행정절차법", "행정심판법", "행정소송법", "영업정지", "국가공무원법"],
    # 지식재산(IP) 분야 핵심 키워드
    "ip": [
        "특허법",
        "상표법",
        "저작권법",
        "디자인보호법",
        "부정경쟁방지 및 영업비밀보호에 관한 법률",
        "실용신안법",
        "영업비밀",
        "저작권 침해",
        "특허침해"
    ],
}

CRIMINAL_EXCLUDE_LAW_NAMES = {"도로교통법", "군사기밀 보호법", "군사기밀보호법"}


def collect_legal_type_documents(
    legal_type: LegalType,
    keywords: list[str] | None = None,
    max_results_per_keyword: int = 5,
    sleep_sec: float = 0.3,
) -> list[LegalDocument]:
    """키워드 리스트로 4개 카테고리(법령/판례/해석례/결정례)를 전부 API로 수집."""
    keywords = keywords or DEFAULT_KEYWORDS[legal_type]
    all_docs: list[LegalDocument] = []

    for kw in tqdm(keywords, desc=f"[{legal_type}] 키워드별 수집"):
        try:
            statute_hits = search_statute(kw, display=max_results_per_keyword)
        except Exception as e:
            logger.error(f"[법령] '{kw}' 검색 실패, 건너뜀: {e}")
            statute_hits = []
        for hit in statute_hits:
            if not hit["법령ID"]:
                continue
            try:
                all_docs.extend(fetch_statute_from_api(hit["법령ID"], legal_type, mst=hit["MST"]))
            except Exception as e:
                logger.error(f"[법령] {hit.get('법령명')} ({hit['법령ID']}) 수집 실패, 건너뜀: {e}")
            time.sleep(sleep_sec)

        try:
            prec_hits = search_precedent(kw, display=max_results_per_keyword)
        except Exception as e:
            logger.error(f"[판례] '{kw}' 검색 실패, 건너뜀: {e}")
            prec_hits = []
        for hit in prec_hits:
            if not hit["판례일련번호"]:
                continue
            try:
                all_docs.extend(fetch_precedent_from_api(hit["판례일련번호"], legal_type))
            except Exception as e:
                logger.error(f"[판례] {hit['판례일련번호']} 수집 실패, 건너뜀: {e}")
            time.sleep(sleep_sec)

        try:
            expc_hits = search_interpretation(kw, display=max_results_per_keyword)
        except Exception as e:
            logger.error(f"[해석례] '{kw}' 검색 실패, 건너뜀: {e}")
            expc_hits = []
        for hit in expc_hits:
            interp_id = hit["법령해석례일련번호"]
            if not interp_id:
                continue
            try:
                all_docs.extend(fetch_interpretation_from_api(interp_id, legal_type))
            except Exception as e:
                logger.error(f"[해석례] {interp_id} 수집 실패, 건너뜀: {e}")
            time.sleep(sleep_sec)

        try:
            detc_hits = search_decision(kw, display=max_results_per_keyword)
        except Exception as e:
            logger.error(f"[결정례] '{kw}' 검색 실패, 건너뜀: {e}")
            detc_hits = []
        for hit in detc_hits:
            if not hit["헌재결정례일련번호"]:
                continue
            try:
                all_docs.extend(fetch_decision_from_api(hit["헌재결정례일련번호"], legal_type))
            except Exception as e:
                logger.error(f"[결정례] {hit['헌재결정례일련번호']} 수집 실패, 건너뜀: {e}")
            time.sleep(sleep_sec)

    if legal_type == "criminal":
        before = len(all_docs)
        all_docs = [d for d in all_docs if d.law_name not in CRIMINAL_EXCLUDE_LAW_NAMES]
        removed = before - len(all_docs)
        if removed:
            logger.warning(f"[criminal] 무관 법령 {removed}개 청크 제외 (CRIMINAL_EXCLUDE_LAW_NAMES 기준)")

    logger.info(f"[{legal_type}] 전체 수집 완료: {len(all_docs)}개 청크")
    return all_docs


# ──────────────────────────────────────────────────────────────
# 8. 인덱스 빌드 & 저장/로딩
# ──────────────────────────────────────────────────────────────

_EMBEDDINGS_CACHE: dict[str, "HuggingFaceEmbeddings"] = {}


def _get_embeddings(model_name: str, device: str = "cpu"):
    """프로세스 내에서 동일 (model_name, device) 조합은 1번만 로드해서 공유한다."""
    cache_key = f"{model_name}::{device}"
    if cache_key not in _EMBEDDINGS_CACHE:
        logger.info(f"임베딩 모델 로딩 중: {model_name} (device={device}, 프로세스 전체에서 1회만 로드)")
        _EMBEDDINGS_CACHE[cache_key] = HuggingFaceEmbeddings(
            model_name=model_name,
            model_kwargs={"device": device},
            encode_kwargs={"normalize_embeddings": True},
        )
    return _EMBEDDINGS_CACHE[cache_key]


def build_indexes(
    documents: list[LegalDocument],
    legal_type: LegalType,
    index_dir: str | Path | None = None,
    embedding_model_name: str | None = None,
    embedding_device: str | None = None,
) -> tuple[FAISS, BM25Retriever]:
    if not documents:
        raise ValueError("빈 문서 리스트로는 인덱스를 만들 수 없습니다.")

    index_dir = Path(index_dir or os.getenv("INDEX_DIR", "./indexes")) / legal_type
    index_dir.mkdir(parents=True, exist_ok=True)

    embedding_model_name = embedding_model_name or os.getenv("EMBEDDING_MODEL_NAME", "BAAI/bge-m3")
    embedding_device = embedding_device or os.getenv("EMBEDDING_DEVICE", "cpu")
    lc_docs = [d.to_langchain_document() for d in documents]

    embeddings = _get_embeddings(embedding_model_name, embedding_device)

    logger.info(f"[{legal_type}] FAISS 인덱스 생성 중... ({len(lc_docs)}개 청크)")
    faiss_index = FAISS.from_documents(lc_docs, embeddings)
    faiss_index.save_local(str(index_dir / "faiss"))

    logger.info(f"[{legal_type}] BM25 인덱스 생성 중...")
    bm25_retriever = BM25Retriever.from_documents(lc_docs)
    bm25_retriever.k = 5

    with open(index_dir / "bm25_docs.json", "w", encoding="utf-8") as f:
        json.dump([asdict(d) for d in documents], f, ensure_ascii=False)

    logger.info(f"[{legal_type}] 인덱스 저장 완료: {index_dir}")
    return faiss_index, bm25_retriever


def load_indexes(
    legal_type: LegalType,
    index_dir: str | Path | None = None,
    embedding_model_name: str | None = None,
    embedding_device: str | None = None,
) -> tuple[FAISS, BM25Retriever]:
    index_dir = Path(index_dir or os.getenv("INDEX_DIR", "./indexes")) / legal_type
    embedding_model_name = embedding_model_name or os.getenv("EMBEDDING_MODEL_NAME", "BAAI/bge-m3")
    embedding_device = embedding_device or os.getenv("EMBEDDING_DEVICE", "cpu")

    embeddings = _get_embeddings(embedding_model_name, embedding_device)
    faiss_index = FAISS.load_local(
        str(index_dir / "faiss"), embeddings, allow_dangerous_deserialization=True
    )

    with open(index_dir / "bm25_docs.json", encoding="utf-8") as f:
        raw_docs = json.load(f)
    lc_docs = [LegalDocument(**d).to_langchain_document() for d in raw_docs]
    bm25_retriever = BM25Retriever.from_documents(lc_docs)
    bm25_retriever.k = 5

    return faiss_index, bm25_retriever


# ──────────────────────────────────────────────────────────────
# 9. CLI
# ──────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="법률 RAG 인덱스 빌더 (국가법령정보센터 Open API 전용)")
    parser.add_argument("--legal-type", choices=["civil", "criminal", "administrative", "ip"])
    parser.add_argument("--keywords", nargs="*", help="검색 키워드 목록 (생략하면 기본 키워드 사용)")
    parser.add_argument("--max-per-keyword", type=int, default=5, help="키워드당 카테고리별 최대 수집 건수")
    parser.add_argument("--debug", metavar="QUERY", help="본문 파싱 없이 원본 XML만 출력하고 종료")
    parser.add_argument("--debug-target", default="law", choices=["law", "prec", "expc", "detc"])
    args = parser.parse_args()

    if args.debug:
        debug_raw_response(args.debug_target, args.debug)
        raise SystemExit(0)

    if not args.legal_type:
        parser.error("--legal-type is required unless --debug is used")

    docs = collect_legal_type_documents(
        args.legal_type, keywords=args.keywords, max_results_per_keyword=args.max_per_keyword
    )
    if not docs:
        raise SystemExit("수집된 문서가 없습니다. 키워드나 OC 키를 확인하세요.")

    build_indexes(docs, legal_type=args.legal_type)
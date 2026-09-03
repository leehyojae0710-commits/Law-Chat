package com.lawchat.domain.precedent.dto.response;

import com.lawchat.domain.precedent.dto.projection.PrecedentSummaryView;
import com.lawchat.domain.precedent.entity.Precedent;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * GET /api/precedents 목록/검색 결과.
 * 프론트 features/precedent-search/types.ts 의 Precedent[] + 페이지 정보에 대응.
 */
public record PrecedentListResponse(
        List<PrecedentSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PrecedentListResponse from(Page<Precedent> resultPage) {
        List<PrecedentSummaryResponse> items = resultPage.getContent().stream()
                .map(PrecedentSummaryResponse::from)
                .toList();
        return new PrecedentListResponse(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    /**
     * PrecedentRepository#searchSummary (LOB 컬럼을 뺀 프로젝션) 결과용.
     * 목록/검색은 이제 이 경로를 탄다 — from(Page<Precedent>)는 프로젝션이 커버 못 하는
     * 곳(있다면)을 위해 남겨둔다.
     */
    public static PrecedentListResponse fromSummaryView(Page<PrecedentSummaryView> resultPage) {
        List<PrecedentSummaryResponse> items = resultPage.getContent().stream()
                .map(PrecedentSummaryResponse::fromView)
                .toList();
        return new PrecedentListResponse(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    /**
     * 목록 카드(PrecedentResultCard)용 요약. 프론트 Precedent 타입과 1:1 대응.
     * decidedDate(선고일자)는 Precedent.decidedDate를 "yyyy-MM-dd" 문자열로 내려준다.
     * 값이 없는 판례(재동기화 전 기존 데이터 등)는 빈 문자열("")로 내려간다 — 프론트에서 fallback 처리 필요.
     */
    public record PrecedentSummaryResponse(
            String id,
            String court,
            String decidedDate,
            String caseNumber,
            String title,
            String summary,
            String category
    ) {
        public static PrecedentSummaryResponse from(Precedent p) {
            return new PrecedentSummaryResponse(
                    String.valueOf(p.getPrecedentId()),
                    p.getCourtName(),
                    p.getDecidedDate() != null ? p.getDecidedDate().toString() : "",
                    p.getCaseNumber(),
                    p.getCaseName(),
                    p.getSummary(),
                    p.getCaseTypeName()
            );
        }

        public static PrecedentSummaryResponse fromView(PrecedentSummaryView v) {
            return new PrecedentSummaryResponse(
                    String.valueOf(v.getPrecedentId()),
                    v.getCourtName(),
                    v.getDecidedDate() != null ? v.getDecidedDate().toString() : "",
                    v.getCaseNumber(),
                    v.getCaseName(),
                    v.getSummary(),
                    v.getCaseTypeName()
            );
        }
    }
}

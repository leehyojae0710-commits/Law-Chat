package com.lawchat.domain.precedent.dto.response;

import com.lawchat.domain.precedent.entity.Precedent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET /api/precedents/{id} 상세 조회 응답.
 * 목록 카드보다 상세 필드(참조조문/참조판례/전문)까지 포함하고,
 * 로그인 사용자라면 북마크 여부(isBookmarked)도 함께 내려준다(SavedPrecedentPanel의 저장 버튼 상태용).
 */
public record PrecedentDetailResponse(
        String id,
        String caseNumber,
        String title,
        String court,
        LocalDate decidedDate,
        String category,
        String holding,
        String summary,
        String referencedArticles,
        String referencedCases,
        String fullText,
        LocalDateTime syncedAt,
        boolean isBookmarked
) {
    public static PrecedentDetailResponse of(Precedent p, boolean isBookmarked) {
        return new PrecedentDetailResponse(
                String.valueOf(p.getPrecedentId()),
                p.getCaseNumber(),
                p.getCaseName(),
                p.getCourtName(),
                p.getDecidedDate(),
                p.getCaseTypeName(),
                p.getHolding(),
                p.getSummary(),
                p.getReferencedArticles(),
                p.getReferencedCases(),
                p.getFullText(),
                p.getSyncedAt(),
                isBookmarked
        );
    }
}

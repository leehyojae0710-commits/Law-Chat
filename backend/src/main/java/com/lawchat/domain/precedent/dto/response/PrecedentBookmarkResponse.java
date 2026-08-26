package com.lawchat.domain.precedent.dto.response;

import com.lawchat.domain.precedent.entity.Precedent;
import com.lawchat.domain.precedent.entity.PrecedentBookmark;

import java.time.LocalDateTime;

/**
 * GET /api/precedents/bookmarks (SavedPrecedentPanel) 목록용.
 */
public record PrecedentBookmarkResponse(
        Long bookmarkId,
        String precedentId,
        String court,
        String caseNumber,
        String title,
        String category,
        LocalDateTime bookmarkedAt
) {
    public static PrecedentBookmarkResponse from(PrecedentBookmark bookmark) {
        Precedent precedent = bookmark.getPrecedent();
        return new PrecedentBookmarkResponse(
                bookmark.getBookmarkId(),
                String.valueOf(precedent.getPrecedentId()),
                precedent.getCourtName(),
                precedent.getCaseNumber(),
                precedent.getCaseName(),
                precedent.getCaseTypeName(),
                bookmark.getCreatedAt()
        );
    }
}
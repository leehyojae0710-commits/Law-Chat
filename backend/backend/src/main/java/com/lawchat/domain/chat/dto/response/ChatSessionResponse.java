package com.lawchat.domain.chat.dto.response;

import com.lawchat.domain.chat.entity.ChatSession;

import java.time.LocalDateTime;

/**
 * ChatSidebar / ChatHistoryList / FavoritesList 목록 렌더링용
 */
public record ChatSessionResponse(
        Long sessionId,
        String title,
        Boolean isFavorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getSessionId(),
                session.getTitle(),
                session.getIsFavorite(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}

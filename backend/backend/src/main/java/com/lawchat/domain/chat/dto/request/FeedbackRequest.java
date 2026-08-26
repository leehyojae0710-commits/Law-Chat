package com.lawchat.domain.chat.dto.request;

/**
 * FeedbackModal 컴포넌트에서 사용
 * POST /api/chat/messages/{messageId}/feedback
 */
public record FeedbackRequest(
        Boolean isPositive, // true: 좋아요, false: 싫어요
        String reason
) {
}

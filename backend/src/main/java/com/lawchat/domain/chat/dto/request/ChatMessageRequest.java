package com.lawchat.domain.chat.dto.request;

/**
 * NewChatInput 컴포넌트에서 메시지 전송 시 사용
 * POST /api/chat/sessions/{sessionId}/messages
 */
public record ChatMessageRequest(
        String content
) {
}

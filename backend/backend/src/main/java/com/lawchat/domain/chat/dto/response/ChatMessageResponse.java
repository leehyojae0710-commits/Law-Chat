package com.lawchat.domain.chat.dto.response;

import com.lawchat.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프론트 features/chat/types.ts 의 ChatMessage 와 1:1 대응
 * role은 ChatRole.toFrontendValue() 로 변환해서 내려준다 ("ai" -> "assistant")
 */
public record ChatMessageResponse(
        Long id,
        String role,
        String content,
        List<LegalSourceResponse> sources,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse of(ChatMessage message, List<LegalSourceResponse> sources) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getRole().toFrontendValue(),
                message.getContent(),
                sources,
                message.getCreatedAt()
        );
    }
}

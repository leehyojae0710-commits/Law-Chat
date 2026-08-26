package com.lawchat.domain.chat.dto.response;

/**
 * 프론트 features/chat/types.ts 의 LegalSource 와 1:1 대응
 */
public record LegalSourceResponse(
        String lawName,
        String articleNumber,
        String url
) {
}

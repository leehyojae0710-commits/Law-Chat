package com.lawchat.domain.chat.dto.response;

/**
 * 프론트 features/chat/types.ts 의 LegalSource 와 1:1 대응
 * caseNum: 판례일 때만 채워짐(예: "2025다220329"). 법령 조문일 때는 빈 문자열.
 */
public record LegalSourceResponse(
        String lawName,
        String articleNumber,
        String caseNum,
        String url
) {
}

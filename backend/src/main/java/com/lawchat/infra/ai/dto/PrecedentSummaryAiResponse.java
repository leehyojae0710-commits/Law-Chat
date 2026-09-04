package com.lawchat.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * legal_chatbot_ai(main.py) POST /summarize/precedent 응답 바디.
 * FastAPI SummarizeResponse 와 1:1 대응.
 *
 *   class SummarizeResponse(BaseModel):
 *       summary: str               # KoBART 원본 요약
 *       plain_summary: str | None  # plain=true일 때만 채워짐 (용어 풀이 버전)
 */
public record PrecedentSummaryAiResponse(

        @JsonProperty("summary")
        String summary,

        @JsonProperty("plain_summary")
        String plainSummary
) {
}

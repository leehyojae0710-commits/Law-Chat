package com.lawchat.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * legal_chatbot_ai(main.py) POST /summarize/precedent 요청 바디.
 * FastAPI SummarizeRequest 와 1:1 대응.
 *
 *   class SummarizeRequest(BaseModel):
 *       text: str          # 판례 원문 (백엔드 Precedent.fullText)
 *       plain: bool = True # true면 쉬운 설명(plain_summary)도 같이 반환
 */
public record PrecedentSummaryAiRequest(

        @JsonProperty("text")
        String text,

        @JsonProperty("plain")
        boolean plain
) {
    public static PrecedentSummaryAiRequest of(String fullText) {
        return new PrecedentSummaryAiRequest(fullText, true);
    }
}

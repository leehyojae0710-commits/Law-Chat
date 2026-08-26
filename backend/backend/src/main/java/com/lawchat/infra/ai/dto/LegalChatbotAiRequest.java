package com.lawchat.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * legal_chatbot_ai(main.py) POST /chat/auto 요청 바디.
 * FastAPI AutoChatRequest 와 1:1 대응.
 *
 *   class AutoChatRequest(BaseModel):
 *       text: str
 *       instruction: str | None = None
 */
public record LegalChatbotAiRequest(

        @JsonProperty("text")
        String text,

        @JsonProperty("instruction")
        String instruction
) {
    public static LegalChatbotAiRequest of(String text) {
        return new LegalChatbotAiRequest(text, null);
    }

    public static LegalChatbotAiRequest withInstruction(String text, String instruction) {
        return new LegalChatbotAiRequest(text, instruction);
    }
}

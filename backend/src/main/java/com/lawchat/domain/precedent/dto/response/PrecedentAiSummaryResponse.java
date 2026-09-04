package com.lawchat.domain.precedent.dto.response;

import com.lawchat.infra.ai.dto.PrecedentSummaryAiResponse;

/**
 * GET /api/precedents/{id}/ai-summary 응답.
 * legal_chatbot_ai(main.py) POST /summarize/precedent 결과를 그대로 내려준다 (DB에 저장하지 않음 - 매 요청 실시간 생성).
 * PrecedentResultCard의 "AI 요약 보기" 버튼용.
 */
public record PrecedentAiSummaryResponse(
        String summary,
        String plainSummary
) {
    public static PrecedentAiSummaryResponse from(PrecedentSummaryAiResponse aiResponse) {
        return new PrecedentAiSummaryResponse(aiResponse.summary(), aiResponse.plainSummary());
    }
}

package com.lawchat.infra.ai.client;

import com.lawchat.infra.ai.dto.PrecedentSummaryAiRequest;
import com.lawchat.infra.ai.dto.PrecedentSummaryAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * legal_chatbot_ai(main.py, AWS EC2) 판례요약(KoBART) 엔드포인트 연동 클라이언트.
 *
 * main.py: POST /summarize/precedent
 *   - 같은 FastAPI 프로세스, 같은 base-url(ai.legal-chatbot.base-url)을 쓰므로
 *     LegalChatbotClient와 동일한 legalChatbotRestClient 빈을 재사용한다.
 *   - 챗봇(LoRA) 모델과 완전히 다른 KoBART 모델을 쓰므로 클래스는 분리해서 관리한다.
 *   - kobart 체크포인트가 없으면 AI 서버가 503을 반환한다 (main.py kobart_ready() 참고).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrecedentSummaryClient {

    private static final String SUMMARIZE_PATH = "/summarize/precedent";

    private final RestClient legalChatbotRestClient;

    public PrecedentSummaryAiResponse summarize(String fullText) {
        PrecedentSummaryAiRequest request = PrecedentSummaryAiRequest.of(fullText);

        try {
            return legalChatbotRestClient.post()
                    .uri(SUMMARIZE_PATH)
                    .body(request)
                    .retrieve()
                    .body(PrecedentSummaryAiResponse.class);
        } catch (RestClientResponseException e) {
            // 503: kobart 체크포인트 미로딩 / 400: 원문 비어있음 등 - main.py의 HTTPException 그대로 전달됨
            log.error("legal_chatbot_ai 판례요약 호출 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PrecedentSummaryClientException("AI 판례요약 서버 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            // 커넥션 실패, 타임아웃 등 (EC2 재시작 중, 보안그룹 차단 등)
            log.error("legal_chatbot_ai 판례요약 연결 실패", e);
            throw new PrecedentSummaryClientException("AI 판례요약 서버에 연결할 수 없습니다.", e);
        }
    }

    public static class PrecedentSummaryClientException extends RuntimeException {
        public PrecedentSummaryClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

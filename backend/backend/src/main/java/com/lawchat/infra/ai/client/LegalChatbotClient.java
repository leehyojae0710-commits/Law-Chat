package com.lawchat.infra.ai.client;

import com.lawchat.infra.ai.dto.LegalChatbotAiRequest;
import com.lawchat.infra.ai.dto.LegalChatbotAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * legal_chatbot_ai(main.py, AWS EC2) 연동 클라이언트.
 *
 * main.py의 3개 엔드포인트 중 /chat/auto만 사용한다.
 *   - /health               : 헬스체크
 *   - /chat/auto            : ✅ 사용 - 무상태(stateless). 도메인 자동분류 + 답변 + sources 반환
 *   - /chat/simple(/reset)  : ❌ 사용 금지 - AI 서버 자체 인메모리 세션(테스트 전용, ENABLE_TEST_ENDPOINTS
 *                             미설정 시 403). 우리는 chat_sessions/chat_messages로 세션을 정식 관리하므로
 *                             대화 맥락은 이 클라이언트를 호출하는 쪽(ChatService)에서 instruction에 실어 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegalChatbotClient {

    private static final String CHAT_AUTO_PATH = "/chat/auto";
    private static final String HEALTH_PATH = "/health";

    private final RestClient legalChatbotRestClient;

    /**
     * 대화 맥락(이전 턴 요약 등)이 필요 없는 단발 질문.
     */
    public LegalChatbotAiResponse ask(String text) {
        return ask(text, null);
    }

    /**
     * 대화 맥락을 instruction에 실어서 보내는 버전.
     * ChatService에서 chat_messages 최근 N턴을 텍스트로 조립해 instruction으로 넘기면 된다.
     * (main.py의 /chat/simple 내부가 하던 걸 우리가 대신 하는 것)
     */
    public LegalChatbotAiResponse ask(String text, String instruction) {
        LegalChatbotAiRequest request = LegalChatbotAiRequest.withInstruction(text, instruction);

        try {
            return legalChatbotRestClient.post()
                    .uri(CHAT_AUTO_PATH)
                    .body(request)
                    .retrieve()
                    .body(LegalChatbotAiResponse.class);
        } catch (RestClientResponseException e) {
            // 503: 모델 로딩 중(콜드스타트) / 404: 요청한 어댑터 미로드 등 - main.py의 HTTPException 그대로 전달됨
            log.error("legal_chatbot_ai 호출 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LegalChatbotClientException("AI 서버 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            // 커넥션 실패, 타임아웃 등 (EC2 재시작 중, 보안그룹 차단 등)
            log.error("legal_chatbot_ai 연결 실패", e);
            throw new LegalChatbotClientException("AI 서버에 연결할 수 없습니다.", e);
        }
    }

    /**
     * 배포 직후 / 헬스체크 배치용. main.py: GET /health -> {"status": "ok", "groups": {...}, ...}
     */
    public boolean isHealthy() {
        try {
            legalChatbotRestClient.get()
                    .uri(HEALTH_PATH)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("legal_chatbot_ai 헬스체크 실패", e);
            return false;
        }
    }

    public static class LegalChatbotClientException extends RuntimeException {
        public LegalChatbotClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

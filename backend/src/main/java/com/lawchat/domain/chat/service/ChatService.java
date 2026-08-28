package com.lawchat.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawchat.domain.chat.dto.request.ChatMessageRequest;
import com.lawchat.domain.chat.dto.response.ChatMessageResponse;
import com.lawchat.domain.chat.dto.response.ChatSessionResponse;
import com.lawchat.domain.chat.dto.response.LegalSourceResponse;
import com.lawchat.domain.chat.entity.ChatMessage;
import com.lawchat.domain.chat.entity.ChatRole;
import com.lawchat.domain.chat.entity.ChatSession;
import com.lawchat.domain.chat.repository.ChatMessageRepository;
import com.lawchat.domain.chat.repository.ChatSessionRepository;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.infra.ai.client.LegalChatbotClient;
import com.lawchat.infra.ai.dto.LegalChatbotAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * chat_sessions / chat_messages 도메인 서비스
 * - 세션: 생성/목록/즐겨찾기/이름변경/삭제 (ChatSidebar, ChatHistoryList, FavoritesList)
 * - 메시지: 조회/전송 (MessageBubble, NewChatInput)
 *
 * AI 응답은 infra.ai.client.LegalChatbotClient -> legal_chatbot_ai(main.py) POST /chat/auto 로 생성한다.
 * AI 서버는 무상태(stateless)이므로 대화 맥락(이전 턴)은 여기서 DB 이력을 조립해 instruction으로 넘긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final String ACTIVE = "ACTIVE";

    // main.py의 SESSION_MAX_TURNS_IN_PROMPT(=4)와 동일 기준으로 맞춤 (질문+답변 쌍 4개)
    private static final int HISTORY_TURNS_IN_PROMPT = 4;

    private static final String AI_ERROR_FALLBACK_MESSAGE =
            "죄송합니다. 일시적으로 답변을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final LegalChatbotClient legalChatbotClient;

    // ===== 세션 =====

    @Transactional
    public ChatSessionResponse createSession(Long userId, String title) {
        User user = findUserOrThrow(userId);
        ChatSession session = ChatSession.create(user, title);
        chatSessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    public List<ChatSessionResponse> getSessions(Long userId) {
        return chatSessionRepository.findByUser_UserIdAndStatusOrderByUpdatedAtDesc(userId, ACTIVE)
                .stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    public List<ChatSessionResponse> getFavoriteSessions(Long userId) {
        return chatSessionRepository.findByUser_UserIdAndIsFavoriteTrueAndStatusOrderByUpdatedAtDesc(userId, ACTIVE)
                .stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    @Transactional
    public ChatSessionResponse renameSession(Long userId, Long sessionId, String newTitle) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);
        session.rename(newTitle);
        return ChatSessionResponse.from(session);
    }

    @Transactional
    public ChatSessionResponse toggleFavorite(Long userId, Long sessionId) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);
        session.toggleFavorite();
        return ChatSessionResponse.from(session);
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);
        session.delete();
    }

    // ===== 메시지 =====

    public List<ChatMessageResponse> getMessages(Long userId, Long sessionId) {
        findOwnedSessionOrThrow(sessionId, userId); // 소유권 검증
        return chatMessageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(message -> ChatMessageResponse.of(message, parseSources(message.getSources())))
                .toList();
    }

    /**
     * 사용자 메시지 저장 + AI 응답 생성/저장까지 한 번에 처리.
     * legal_chatbot_ai는 무상태라, 직전 대화 이력을 instruction 문자열로 조립해서 함께 보낸다
     * (main.py /chat/simple이 인메모리로 하던 걸 우리는 DB 이력 기반으로 재현).
     *
     * AI 서버 호출이 실패해도(콜드스타트/타임아웃 등) 사용자 메시지 저장은 유지하고,
     * AI 메시지는 사용자에게 보여줄 안내 문구로 대체해 저장한다.
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, ChatMessageRequest request) {
        ChatSession session = findOwnedSessionOrThrow(sessionId, userId);

        List<ChatMessage> priorMessages = chatMessageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);

        ChatMessage userMessage = ChatMessage.createUserMessage(session, request.content());
        chatMessageRepository.save(userMessage);

        String instruction = buildHistoryInstruction(priorMessages);

        String answer;
        String sourcesJson;
        try {
            LegalChatbotAiResponse aiResponse = legalChatbotClient.ask(request.content(), instruction);
            answer = aiResponse.answer();
            sourcesJson = serializeSources(aiResponse);
        } catch (LegalChatbotClient.LegalChatbotClientException e) {
            log.error("AI 서버 호출 실패로 안내 메시지로 대체합니다. sessionId={}", sessionId, e);
            answer = AI_ERROR_FALLBACK_MESSAGE;
            sourcesJson = null;
        }

        ChatMessage aiMessage = ChatMessage.createAiMessage(session, answer, sourcesJson);
        chatMessageRepository.save(aiMessage);

        return ChatMessageResponse.of(aiMessage, parseSources(aiMessage.getSources()));
    }

    // ===== 헬퍼 =====

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. userId=" + userId));
    }

    private ChatSession findOwnedSessionOrThrow(Long sessionId, Long userId) {
        return chatSessionRepository.findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없거나 존재하지 않는 세션입니다. sessionId=" + sessionId));
    }

    private List<LegalSourceResponse> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(sourcesJson, new TypeReference<List<LegalSourceResponse>>() {});
        } catch (JsonProcessingException e) {
            log.warn("sources JSON 역직렬화 실패: {}", sourcesJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * legal_chatbot_ai는 세션을 기억하지 못하는 무상태 서버이므로,
     * DB에 저장된 직전 대화(최근 HISTORY_TURNS_IN_PROMPT턴)를 텍스트로 조립해
     * AutoChatRequest.instruction에 실어 보낸다.
     * priorMessages는 이미 시간순 오름차순으로 조회된 상태를 전제로 한다.
     */
    private String buildHistoryInstruction(List<ChatMessage> priorMessages) {
        if (priorMessages.isEmpty()) {
            return null;
        }

        int maxMessages = HISTORY_TURNS_IN_PROMPT * 2; // 질문+답변 = 1턴
        int fromIndex = Math.max(0, priorMessages.size() - maxMessages);
        List<ChatMessage> recent = priorMessages.subList(fromIndex, priorMessages.size());

        StringBuilder sb = new StringBuilder();
        sb.append("다음은 사용자와의 이전 대화 이력입니다. 이 맥락을 참고하여 이어지는 질문에 답변하시오.\n\n");
        for (ChatMessage message : recent) {
            String speaker = message.getRole() == ChatRole.USER ? "사용자" : "챗봇";
            sb.append(speaker).append(": ").append(message.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * AutoChatResponse.detected_domains[*].sources를 모아서
     * 프론트가 쓰는 LegalSourceResponse(lawName, articleNumber, url) 형태의 JSON 문자열로 변환한다.
     * 도메인이 여러 개일 때는 순서대로 합쳐서 보여준다.
     */
    private String serializeSources(LegalChatbotAiResponse aiResponse) {
        if (aiResponse.detectedDomains() == null || aiResponse.detectedDomains().isEmpty()) {
            return null;
        }

        List<LegalSourceResponse> sources = aiResponse.detectedDomains().stream()
                .filter(domain -> domain.sources() != null)
                .flatMap(domain -> domain.sources().stream())
                .map(source -> new LegalSourceResponse(source.lawName(), source.articleNo(), source.url()))
                .toList();

        if (sources.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.warn("sources 직렬화 실패, sources 없이 저장합니다.", e);
            return null;
        }
    }
}
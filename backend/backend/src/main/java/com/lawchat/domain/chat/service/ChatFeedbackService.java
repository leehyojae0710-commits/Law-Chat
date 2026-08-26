package com.lawchat.domain.chat.service;

import com.lawchat.domain.chat.dto.request.FeedbackRequest;
import com.lawchat.domain.chat.entity.ChatFeedbackDataset;
import com.lawchat.domain.chat.entity.ChatMessage;
import com.lawchat.domain.chat.repository.ChatFeedbackRepository;
import com.lawchat.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * chat_feedback_dataset 도메인 서비스
 * FeedbackModal에서 좋아요/싫어요 + 사유 제출 시,
 * 이후 모델 파인튜닝 데이터셋으로 쓸 수 있도록 prompt/response 스냅샷을 함께 적재한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatFeedbackService {

    private final ChatFeedbackRepository chatFeedbackRepository;
    private final ChatMessageRepository chatMessageRepository;

    public void submitFeedback(Long messageId, FeedbackRequest request) {
        ChatMessage aiMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다. messageId=" + messageId));

        // 싫어요(reason 있음)일 때만 데이터셋 적재 - 정책에 따라 조정 가능
        String prompt = resolvePrompt(aiMessage);

        ChatFeedbackDataset feedback = ChatFeedbackDataset.create(
                messageId,
                prompt,
                aiMessage.getContent(),
                aiMessage.getSources(),
                request.reason()
        );
        chatFeedbackRepository.save(feedback);
    }

    /**
     * TODO: 같은 세션에서 이 AI 메시지 바로 이전의 user 메시지 content를 prompt로 조회하는 로직으로 교체
     */
    private String resolvePrompt(ChatMessage aiMessage) {
        return "";
    }
}

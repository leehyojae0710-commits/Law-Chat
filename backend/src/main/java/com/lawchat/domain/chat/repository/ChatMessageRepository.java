package com.lawchat.domain.chat.repository;

import com.lawchat.domain.chat.entity.ChatMessage;
import com.lawchat.domain.chat.entity.ChatRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // ChatPage/MessageBubble 렌더링용: 세션 내 메시지 시간순 조회
    List<ChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(Long sessionId);

    // 관리자 대시보드의 "싫어요 비율" 분모(전체 답변 수) 계산용. role=AI인 행만 카운트.
    long countByRole(ChatRole role);
}

package com.lawchat.domain.chat.repository;

import com.lawchat.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // ChatPage/MessageBubble 렌더링용: 세션 내 메시지 시간순 조회
    List<ChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(Long sessionId);
}

package com.lawchat.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DB: chat_messages
 * sources: json 컬럼 - 참조 법령/판례 목록을 JSON 문자열로 저장
 * (직렬화/역직렬화는 service 레이어에서 ObjectMapper로 처리)
 *
 * ★ chat_messages 테이블에는 updated_at 컬럼이 없다(created_at만 존재).
 *   BaseTimeEntity(=createdAt+updatedAt)를 상속하면 ddl-auto=validate에서
 *   "missing column updated_at" 로 기동이 실패하므로, createdAt만 직접 들고 있는다.
 *   (chat_feedback_dataset과 동일한 패턴)
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRole role;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(columnDefinition = "json")
    private String sources; // LegalSource[] 를 JSON 문자열로 저장

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatMessage(ChatSession session, ChatRole role, String content, String sources) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.sources = sources;
    }

    public static ChatMessage createUserMessage(ChatSession session, String content) {
        return ChatMessage.builder()
                .session(session)
                .role(ChatRole.USER)
                .content(content)
                .build();
    }

    public static ChatMessage createAiMessage(ChatSession session, String content, String sourcesJson) {
        return ChatMessage.builder()
                .session(session)
                .role(ChatRole.AI)
                .content(content)
                .sources(sourcesJson)
                .build();
    }
}

package com.lawchat.domain.chat.entity;

import com.lawchat.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB: chat_messages
 * sources: json 컬럼 - 참조 법령/판례 목록을 JSON 문자열로 저장
 * (직렬화/역직렬화는 service 레이어에서 ObjectMapper로 처리)
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

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

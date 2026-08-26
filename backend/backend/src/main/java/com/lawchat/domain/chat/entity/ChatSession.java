package com.lawchat.domain.chat.entity;

import com.lawchat.domain.user.entity.User;
import com.lawchat.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB: chat_sessions
 * 프론트: ChatSidebar / ChatHistoryList / FavoritesList 에서 세션 단위로 사용
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, DELETED 등

    @Builder
    private ChatSession(User user, String title) {
        this.user = user;
        this.title = title;
        this.isFavorite = false;
        this.status = "ACTIVE";
    }

    public static ChatSession create(User user, String title) {
        return ChatSession.builder()
                .user(user)
                .title(title)
                .build();
    }

    public void rename(String newTitle) {
        this.title = newTitle;
    }

    public void toggleFavorite() {
        this.isFavorite = !this.isFavorite;
    }

    public void delete() {
        this.status = "DELETED";
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getUserId().equals(userId);
    }
}

package com.lawchat.domain.precedent.entity;

import com.lawchat.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DB: precedent_bookmarks
 * 회원이 저장(북마크)한 판례. SavedPrecedentPanel에서 사용.
 * (user_id, precedent_id) 조합은 유니크 — 같은 판례를 두 번 저장하지 않는다.
 *
 * 주의: 이 테이블은 updated_at 컬럼이 없다(schema.sql 기준, created_at만 존재).
 * 그래서 updated_at까지 있는 BaseTimeEntity를 상속하지 않고 createdAt만 직접 선언한다.
 */
@Entity
@Table(name = "precedent_bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_precedent",
                columnNames = {"user_id", "precedent_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrecedentBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "precedent_id", nullable = false)
    private Precedent precedent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PrecedentBookmark(User user, Precedent precedent) {
        this.user = user;
        this.precedent = precedent;
    }

    public static PrecedentBookmark create(User user, Precedent precedent) {
        return PrecedentBookmark.builder()
                .user(user)
                .precedent(precedent)
                .build();
    }
}
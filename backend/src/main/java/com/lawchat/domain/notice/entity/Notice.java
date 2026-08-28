package com.lawchat.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private NoticeCategory category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * DB의 DEFAULT CURRENT_TIMESTAMP 에 의존하지 않고 Hibernate 가 직접 값을 채운다.
     * (JPA 는 INSERT 시 null 을 명시적으로 보내기 때문에 DB 기본값이 적용되지 않음)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    private Notice(NoticeCategory category, String title, String content, String fileUrl) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.fileUrl = fileUrl;
        this.isPinned = false;
    }

    public static Notice create(NoticeCategory category, String title, String content, String fileUrl) {
        return new Notice(category, title, content, fileUrl);
    }

    /**
     * null 로 넘어온 필드는 변경하지 않는다 (User.updateProfile 패턴과 동일).
     */
    public void update(String title, String content, String fileUrl) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (fileUrl != null) {
            this.fileUrl = fileUrl;
        }
    }

    public void togglePin() {
        this.isPinned = !this.isPinned;
    }
}

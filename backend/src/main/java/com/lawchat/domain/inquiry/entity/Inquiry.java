package com.lawchat.domain.inquiry.entity;

import com.lawchat.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 1:1 문의 엔티티 — inquiries 테이블과 1:1 매핑. DB 스키마 변경 없음.
 *
 * [스키마 특성과 매핑 전략]
 * 1. user_id / admin_id 는 ON DELETE SET NULL 이다.
 *    회원이 탈퇴해도 문의 이력은 남고 작성자 연결만 끊긴다.
 *    따라서 @ManyToOne 의 optional 을 true 로 두고, 조회하는 쪽에서 항상 null 을 고려해야 한다.
 * 2. status 컬럼이 없다. answered_at + is_approved 로 계산한다(getStatus).
 * 3. is_approved 가 NULL 을 허용한다.
 *    primitive boolean 으로 매핑하면 기존 NULL 행을 읽는 순간 예외가 나므로 Boolean 으로 받고,
 *    읽을 때는 isApproved() 로 null 을 false 취급한다.
 * 4. created_at 은 Notice 와 동일하게 @CreationTimestamp 로 Hibernate 가 채운다.
 *    (JPA 는 INSERT 시 null 을 명시적으로 보내 DB 기본값이 적용되지 않기 때문)
 *    inquiries 에는 updated_at 컬럼이 없어 BaseTimeEntity 를 상속하지 않는다.
 */
@Entity
@Getter
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    /** 작성자. 탈퇴 시 null 이 된다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    /** 답변한 관리자. 답변 전이거나 관리자 탈퇴 시 null. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private InquiryCategory category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Lob
    @Column(name = "answer_content")
    private String answerContent;

    /**
     * 업로드된 스크린샷의 "파일명"만 저장한다.
     * 절대 URL 은 응답을 만들 때 FileUrls.view() 로 조립한다.
     * (공지 첨부가 file_url 에 파일명만 담는 것과 동일한 규칙)
     */
    @Column(name = "screenshot_url", length = 512)
    private String screenshotUrl;

    /** 답변 공개 승인 여부. DB가 NULL 을 허용하므로 Boolean. 읽을 때는 isApproved() 사용. */
    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Inquiry(User user, InquiryCategory category, String title,
                    String content, String screenshotUrl) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.screenshotUrl = screenshotUrl;
        this.isApproved = false;
    }

    public static Inquiry create(User user, InquiryCategory category, String title,
                                 String content, String screenshotUrl) {
        return new Inquiry(user, category, title, content, screenshotUrl);
    }

    /**
     * 관리자 답변 등록 · 수정. 등록과 동시에 사용자에게 공개된다.
     * 이미 답변이 있으면 덮어쓰고 answered_at 을 갱신한다.
     */
    public void answer(User admin, String answerContent) {
        this.admin = admin;
        this.answerContent = answerContent;
        this.isApproved = true;
        this.answeredAt = LocalDateTime.now();
    }

    /** 검수가 필요할 때 답변을 다시 감춘다. 사용자 화면에서는 답변대기로 돌아간다. */
    public void hideAnswer() {
        this.isApproved = false;
    }

    /** DB에 없는 파생 상태값. */
    public InquiryStatus getStatus() {
        return (answeredAt != null && isApproved())
                ? InquiryStatus.ANSWERED
                : InquiryStatus.PENDING;
    }

    /** is_approved 가 NULL 인 기존 행도 안전하게 false 로 취급한다. */
    public boolean isApproved() {
        return Boolean.TRUE.equals(this.isApproved);
    }

    /** 탈퇴 회원의 문의는 user 가 null 이므로 어떤 userId 와도 일치하지 않는다. */
    public boolean isOwnedBy(Long candidateUserId) {
        return user != null && user.getUserId().equals(candidateUserId);
    }

    /** 답변이 달리기 전에만 삭제할 수 있다. */
    public boolean isDeletable() {
        return answeredAt == null;
    }
}

package com.lawchat.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DB: chat_feedback_dataset
 * FeedbackModal에서 좋아요/싫어요 + 사유를 남기면 이후 모델 파인튜닝용 데이터셋으로 적재.
 * created_at만 존재 (updated_at 없음) -> BaseTimeEntity 상속하지 않고 단독 필드로 관리.
 */
@Entity
@Table(name = "chat_feedback_dataset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatFeedbackDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "message_id")
    private Long messageId; // FK지만 원본 스키마상 nullable, 연관관계 매핑 없이 id만 보관

    @Lob
    @Column(nullable = false)
    private String prompt;

    @Lob
    @Column(nullable = false)
    private String response;

    @Column(columnDefinition = "json")
    private String sources;

    @Column(length = 255)
    private String reason;

    // ★ @CreatedDate(Spring Data JPA)는 @EnableJpaAuditing 이 켜져 있어야 값이 채워지는데
    //   이 프로젝트엔 그 설정이 없다(JpaConfig.java가 빈 파일). 그 상태로 두면 created_at 이
    //   채워지지 않은 채 INSERT 되어 컬럼의 NOT NULL 제약에 걸려 저장 자체가 실패한다.
    //   Hibernate가 flush 시점에 직접 채워주는 @CreationTimestamp로 바꿔 이 문제를 없앤다.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatFeedbackDataset(Long messageId, String prompt, String response, String sources, String reason) {
        this.messageId = messageId;
        this.prompt = prompt;
        this.response = response;
        this.sources = sources;
        this.reason = reason;
    }

    public static ChatFeedbackDataset create(Long messageId, String prompt, String response, String sources, String reason) {
        return ChatFeedbackDataset.builder()
                .messageId(messageId)
                .prompt(prompt)
                .response(response)
                .sources(sources)
                .reason(reason)
                .build();
    }
}

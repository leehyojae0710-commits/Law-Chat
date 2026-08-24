package com.lawchat.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 생성/수정 시각 컬럼을 공통으로 쓰기 위한 상위 클래스.
 *
 * 이 파일이 비어 있으면(=클래스가 없으면) 이 클래스를 extends 하는
 * ChatSession / ChatMessage 가 애초에 컴파일되지 않는다.
 *
 * User 엔티티가 각자 createdAt/updatedAt 필드를 직접 들고 있는 것과 달리,
 * 여기서는 공통 상위 클래스로 분리해 여러 엔티티가 재사용할 수 있게 한다.
 */
@MappedSuperclass
public abstract class BaseTimeEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

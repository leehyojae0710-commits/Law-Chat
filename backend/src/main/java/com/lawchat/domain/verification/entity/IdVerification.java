package com.lawchat.domain.verification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "id_verifications") // ★ id_verification -> id_verifications 로 변경
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_verification_id")
    private Long id;

    @Column(name = "auth_target", nullable = false, length = 255)
    private String authTarget;

    @Column(name = "auth_code", nullable = false, length = 10)
    private String authCode;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    public IdVerification(String authTarget, String authCode, LocalDateTime expiredAt) {
        this.authTarget = authTarget;
        this.authCode = authCode;
        this.isVerified = false;
        this.attemptCount = 0;
        this.expiredAt = expiredAt;
    }

    public static IdVerification create(ContactType contactType, String contactValue, String code, long ttlMinutes) {
        return IdVerification.builder()
                .authTarget(contactValue)
                .authCode(code)
                .expiredAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .build();
    }

    public boolean isUsable() {
        return !isVerified && LocalDateTime.now().isBefore(this.expiredAt) && this.attemptCount < 5;
    }

    public boolean matches(String code) {
        return this.authCode != null && this.authCode.equals(code);
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public void verify() {
        this.isVerified = true;
        this.usedAt = LocalDateTime.now();
    }

    public void markVerified() {
        verify();
    }
}
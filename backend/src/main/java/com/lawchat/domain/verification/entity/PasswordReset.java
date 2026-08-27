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
@Table(name = "password_reset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

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
    public PasswordReset(Long userId, String authTarget, String authCode, LocalDateTime expiredAt) {
        this.userId = userId;
        this.authTarget = authTarget;
        this.authCode = authCode;
        this.isVerified = false;
        this.attemptCount = 0;
        this.expiredAt = expiredAt;
    }

    public static PasswordReset create(String email, ContactType contactType, String contactValue, String code, long ttlMinutes) {
        return PasswordReset.builder()
                .authTarget(contactValue)
                .authCode(code)
                .expiredAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .build();
    }

    public boolean isCodeUsable() {
        return !isVerified && LocalDateTime.now().isBefore(this.expiredAt) && this.attemptCount < 5;
    }

    public boolean matches(String code) {
        return this.authCode != null && this.authCode.equals(code);
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public void markVerified(String resetToken, long ttlMinutes) {
        this.isVerified = true;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isTokenUsable() {
        return this.isVerified && this.usedAt != null;
    }

    public String getEmail() {
        return this.authTarget;
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
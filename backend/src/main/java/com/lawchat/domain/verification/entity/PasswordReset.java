package com.lawchat.domain.verification.entity;

import com.lawchat.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 인증 요청 1건 — 실제 DB의 password_reset 테이블과 1:1 매핑.
 *
 * ★ 지금은 이메일 전용이다.
 *   auth_target 컬럼은 나중에 전화번호도 담을 수 있게 범용으로 설계했지만,
 *   User 테이블에 phone 컬럼이 아직 없어서 당장은 항상 "이메일 주소"만 들어간다.
 *   나중에 phone 이 추가되면 이 엔티티/테이블은 그대로 두고 서비스 로직만 확장하면 된다.
 *
 * [단일 팩터 + 단일 단계 설계]
 *  코드 확인과 비밀번호 변경이 한 번의 API 호출(POST /password/reset)로 끝난다.
 *  그래서 별도의 resetToken 발급 단계 없이, is_verified 와 used_at 이 같은 시점에 함께 기록된다.
 */
@Entity
@Table(name = "password_reset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordReset {

    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_id")
    private Long resetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 지금은 항상 이메일 주소가 들어간다. (추후 전화번호 지원 시에도 컬럼은 그대로 재사용) */
    @Column(name = "auth_target", nullable = false, length = 255)
    private String authTarget;

    @Column(name = "auth_code", nullable = false, length = 10)
    private String authCode;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    /** 이 코드로 비밀번호 변경까지 완료된 시각. 완료 전에는 null. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public static PasswordReset create(User user, String authTarget, String authCode, long ttlMinutes) {
        PasswordReset reset = new PasswordReset();
        reset.user = user;
        reset.authTarget = authTarget;
        reset.authCode = authCode;
        reset.verified = false;
        reset.attemptCount = 0;
        reset.expiredAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        return reset;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }

    public boolean matches(String inputCode) {
        return this.authCode.equals(inputCode);
    }

    public boolean exceededMaxAttempts() {
        return this.attemptCount >= MAX_ATTEMPTS;
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    /** 아직 검증/사용되지 않았고, 만료되지 않았고, 시도 횟수를 초과하지 않은 상태 */
    public boolean isUsable() {
        return !verified && usedAt == null && !isExpired() && !exceededMaxAttempts();
    }

    /** 코드 확인 성공 + 그 즉시 비밀번호 변경까지 완료 — 단일 단계라 둘을 같이 마킹한다. */
    public void markVerifiedAndUsed() {
        this.verified = true;
        this.usedAt = LocalDateTime.now();
    }
}

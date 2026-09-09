package com.lawchat.domain.verification.repository;

import com.lawchat.domain.verification.entity.IdVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdVerificationRepository extends JpaRepository<IdVerification, Long> {

    Optional<IdVerification> findFirstByAuthTargetAndIsVerifiedFalseOrderByCreatedAtDesc(String authTarget);

    /**
     * [계정 복구] 최근에 이 연락처로 인증을 마쳤는지 확인한다.
     *
     * ★ 왜 인증코드를 다시 받지 않는가
     *   복구는 기존 아이디 찾기 인증(/api/verifications/*)을 그대로 재사용한다.
     *   verifyCode() 가 이미 is_verified=true, used_at=지금 을 찍어 두므로,
     *   복구 시점에는 "그 흔적이 최근 것인지" 만 보면 된다.
     *   같은 인증을 두 번 시키면 사용자만 번거롭고 얻는 것이 없다.
     *
     * ★ 왜 시간을 제한하는가
     *   used_at 조건이 없으면 몇 달 전에 아이디를 찾았던 기록만으로도 복구가 통과한다.
     *   인증한 직후에만 유효하도록 호출부에서 짧은 시간 범위를 넘긴다.
     */
    Optional<IdVerification> findFirstByAuthTargetAndIsVerifiedTrueAndUsedAtAfterOrderByUsedAtDesc(
            String authTarget, LocalDateTime usedAfter);
}

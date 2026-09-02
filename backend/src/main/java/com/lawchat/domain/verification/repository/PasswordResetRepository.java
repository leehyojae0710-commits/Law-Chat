package com.lawchat.domain.verification.repository;

import com.lawchat.domain.verification.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    /**
     * 같은 회원+연락처로 여러 번 요청했을 수 있으므로, 아직 검증되지 않은 것 중
     * 가장 최근 row 하나만 가져온다. "First" 접두사로 Spring Data JPA 가 LIMIT 1 을 붙여준다.
     */
    Optional<PasswordReset> findFirstByUser_UserIdAndAuthTargetAndVerifiedFalseOrderByCreatedAtDesc(
            Long userId, String authTarget);
}

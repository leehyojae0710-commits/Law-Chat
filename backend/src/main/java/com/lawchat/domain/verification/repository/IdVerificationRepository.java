package com.lawchat.domain.verification.repository;

import com.lawchat.domain.verification.entity.IdVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdVerificationRepository extends JpaRepository<IdVerification, Long> {

    Optional<IdVerification> findFirstByAuthTargetAndIsVerifiedFalseOrderByCreatedAtDesc(String authTarget);
}
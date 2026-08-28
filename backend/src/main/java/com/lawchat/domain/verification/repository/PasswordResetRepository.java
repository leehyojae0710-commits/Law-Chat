package com.lawchat.domain.verification.repository;

import com.lawchat.domain.verification.entity.ContactType;
import com.lawchat.domain.verification.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    @Query("SELECT p FROM PasswordReset p WHERE p.authTarget = :contactValue AND p.isVerified = false ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PasswordReset> findFirstByEmailAndContactTypeAndContactValueAndVerifiedFalseOrderByCreatedAtDesc(
            @Param("email") String email,
            @Param("contactType") ContactType contactType,
            @Param("contactValue") String contactValue
    );

    @Query("SELECT p FROM PasswordReset p WHERE p.authTarget = :resetToken ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PasswordReset> findByResetToken(@Param("resetToken") String resetToken);
}
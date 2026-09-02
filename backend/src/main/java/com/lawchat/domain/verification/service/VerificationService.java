package com.lawchat.domain.verification.service;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.domain.verification.dto.request.SendCodeRequest;
import com.lawchat.domain.verification.dto.request.VerifyCodeRequest;
import com.lawchat.domain.verification.dto.response.FindIdResultResponse;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.entity.ContactType;
import com.lawchat.domain.verification.entity.IdVerification;
import com.lawchat.domain.verification.repository.IdVerificationRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.infra.notification.EmailSender;
import com.lawchat.infra.notification.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 아이디(이메일) 찾기 — 전화번호 또는 이메일로 본인 확인 후 로그인 아이디를 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final long CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPT_COUNT = 5;

    private final UserRepository userRepository;
    private final IdVerificationRepository idVerificationRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public VerificationService(UserRepository userRepository,
                               IdVerificationRepository idVerificationRepository,
                               VerificationCodeGenerator codeGenerator,
                               EmailSender emailSender,
                               SmsSender smsSender) {
        this.userRepository = userRepository;
        this.idVerificationRepository = idVerificationRepository;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    // ==================================================================
    // 1) 인증코드 발송
    // ==================================================================

    @Transactional
    public VerificationResultResponse sendCode(SendCodeRequest request) {
        String normalizedValue = normalize(request.contactType(), request.contactValue());

        findUserByContact(request.contactType(), normalizedValue)
                .ifPresentOrElse(
                        user -> issueAndSend(request.contactType(), normalizedValue),
                        () -> log.info("아이디 찾기 요청 - 존재하지 않는 연락처 (조용히 무시): type={}", request.contactType())
                );

        return VerificationResultResponse.ok("입력하신 연락처로 인증코드를 발송했습니다. (가입 정보가 없으면 발송되지 않습니다)");
    }

    private void issueAndSend(ContactType contactType, String contactValue) {
        String code = codeGenerator.generate6Digit();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);

        // id_verification 테이블 스키마 매핑
        IdVerification verification = IdVerification.builder()
                .authTarget(contactValue)
                .authCode(code)
                .expiredAt(expiredAt)
                .build();
        idVerificationRepository.save(verification);

        String subject = "[LawChat] 아이디 찾기 인증코드";
        String body = "인증코드: " + code + " (5분 이내에 입력해 주세요)";

        if (contactType == ContactType.EMAIL) {
            emailSender.send(contactValue, subject, body);
        } else {
            smsSender.send(contactValue, body);
        }
    }

    // ==================================================================
    // 2) 인증코드 확인 → 아이디(이메일) 반환
    // ==================================================================

    @Transactional
    public FindIdResultResponse verifyCode(VerifyCodeRequest request) {
        String normalizedValue = normalize(request.contactType(), request.contactValue());

        // auth_target 기준으로 미검증 최신 row 조회
        IdVerification verification = idVerificationRepository
                .findFirstByAuthTargetAndIsVerifiedFalseOrderByCreatedAtDesc(normalizedValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        // 만료 체크 또는 최대 시도 횟수 초과 체크
        if (LocalDateTime.now().isAfter(verification.getExpiredAt()) || verification.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 인증코드 불일치 체크
        if (!verification.getAuthCode().equals(request.code())) {
            verification.increaseAttempt();
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 인증 성공 처리 (is_verified = true, used_at = now)
        verification.verify();

        User user = findUserByContact(request.contactType(), normalizedValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("아이디 찾기 인증 성공 - userId={}", user.getUserId());

        return FindIdResultResponse.of(user.getEmail());
    }

    // ==================================================================
    // 내부 헬퍼
    // ==================================================================

    private Optional<User> findUserByContact(ContactType contactType, String contactValue) {
        return contactType == ContactType.EMAIL
                ? userRepository.findByEmail(contactValue)
                : userRepository.findByPhone(contactValue);
    }

    private String normalize(ContactType contactType, String value) {
        if (contactType == ContactType.PHONE) {
            return value.replaceAll("[^0-9]", "");
        }
        return value.trim();
    }
}
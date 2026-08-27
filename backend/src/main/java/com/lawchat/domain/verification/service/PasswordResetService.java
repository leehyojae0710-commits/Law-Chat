package com.lawchat.domain.verification.service;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.domain.verification.dto.request.PasswordResetRequest;
import com.lawchat.domain.verification.dto.request.PasswordResetSendCodeRequest;
import com.lawchat.domain.verification.dto.request.PasswordResetVerifyCodeRequest;
import com.lawchat.domain.verification.dto.response.PasswordResetTokenResponse;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.entity.ContactType;
import com.lawchat.domain.verification.entity.PasswordReset;
import com.lawchat.domain.verification.repository.PasswordResetRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.infra.notification.EmailSender;
import com.lawchat.infra.notification.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비밀번호 재설정 — 아이디(이메일) + 그 계정에 등록된 연락처(이메일/전화번호)로 본인 확인 후 재설정.
 *
 * [흐름 — 3단계]
 *  1. sendCode()   : email + contactType/contactValue 가 실제로 같은 계정 소유인지 확인 후 코드 발송
 *  2. verifyCode() : 코드 확인 → 성공 시 resetToken 발급(코드는 여기서 폐기, 이후로는 토큰만 유효)
 *  3. reset()      : resetToken + 새 비밀번호로 실제 변경
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long CODE_TTL_MINUTES = 5;
    private static final long RESET_TOKEN_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetRepository passwordResetRepository,
                                VerificationCodeGenerator codeGenerator,
                                EmailSender emailSender,
                                SmsSender smsSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================================================================
    // 1) 인증코드 발송
    // ==================================================================

    /**
     * ★ 계정 열거 공격 방지
     *   email 이 없거나, email 은 있어도 contactValue 가 그 계정의 실제 email/phone 과
     *   다르면(=본인이 아니면) 조용히 무시하고 동일한 성공 응답을 준다.
     *   또한 소셜 전용 계정(비밀번호가 없는 회원)은 애초에 재설정 대상이 아니므로 제외한다.
     */
    @Transactional
    public VerificationResultResponse sendCode(PasswordResetSendCodeRequest request) {

        String normalizedValue = normalize(request.contactType(), request.contactValue());

        userRepository.findByEmail(request.email())
                .filter(user -> !user.isSocialUser() && user.getPassword() != null)
                .filter(user -> matchesContact(user, request.contactType(), normalizedValue))
                .ifPresentOrElse(
                        user -> issueAndSend(request.email(), request.contactType(), normalizedValue),
                        () -> log.info("비밀번호 재설정 요청 - 일치하는 회원 없음 (조용히 무시): email={}", request.email())
                );

        return VerificationResultResponse.ok("입력하신 정보가 일치하면 인증코드를 발송했습니다.");
    }

    private boolean matchesContact(User user, ContactType contactType, String normalizedValue) {
        String stored = contactType == ContactType.EMAIL ? user.getEmail() : user.getPhone();
        return stored != null && stored.equals(normalizedValue);
    }

    private void issueAndSend(String email, ContactType contactType, String contactValue) {
        String code = codeGenerator.generate6Digit();
        PasswordReset reset = PasswordReset.create(email, contactType, contactValue, code, CODE_TTL_MINUTES);
        passwordResetRepository.save(reset);

        String subject = "[LawChat] 비밀번호 재설정 인증코드";
        String body = "인증코드: " + code + " (5분 이내에 입력해 주세요)";

        if (contactType == ContactType.EMAIL) {
            emailSender.send(contactValue, subject, body);
        } else {
            smsSender.send(contactValue, body);
        }
    }

    // ==================================================================
    // 2) 인증코드 확인 → resetToken 발급
    // ==================================================================

    @Transactional
    public PasswordResetTokenResponse verifyCode(PasswordResetVerifyCodeRequest request) {

        String normalizedValue = normalize(request.contactType(), request.contactValue());

        PasswordReset reset = passwordResetRepository
                .findFirstByEmailAndContactTypeAndContactValueAndVerifiedFalseOrderByCreatedAtDesc(
                        request.email(), request.contactType(), normalizedValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        if (!reset.isCodeUsable()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!reset.matches(request.code())) {
            reset.increaseAttempt();
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        String resetToken = UUID.randomUUID().toString();
        reset.markVerified(resetToken, RESET_TOKEN_TTL_MINUTES);

        log.info("비밀번호 재설정 코드 인증 성공 - email={}", request.email());

        return new PasswordResetTokenResponse(resetToken, RESET_TOKEN_TTL_MINUTES * 60);
    }

    // ==================================================================
    // 3) 실제 비밀번호 변경
    // ==================================================================

    @Transactional
    public void reset(PasswordResetRequest request) {

        PasswordReset reset = passwordResetRepository.findByResetToken(request.resetToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (!reset.isTokenUsable()) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        User user = userRepository.findByEmail(reset.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        reset.markUsed(); // 토큰 재사용 방지

        log.info("비밀번호 재설정 완료 - userId={}", user.getUserId());
    }

    // ==================================================================
    // 내부 헬퍼
    // ==================================================================

    private String normalize(ContactType contactType, String value) {
        if (contactType == ContactType.PHONE) {
            return value.replaceAll("[^0-9]", "");
        }
        return value.trim();
    }
}
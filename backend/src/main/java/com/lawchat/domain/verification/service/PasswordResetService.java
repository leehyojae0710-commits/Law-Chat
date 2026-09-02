package com.lawchat.domain.verification.service;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.domain.verification.dto.request.PasswordResetRequest;
import com.lawchat.domain.verification.dto.request.PasswordResetSendCodeRequest;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.entity.PasswordReset;
import com.lawchat.domain.verification.repository.PasswordResetRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.infra.notification.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정 — 지금은 이메일 전용.
 * (전화번호 지원은 User.phone 컬럼이 추가된 뒤 별도로 확장 예정)
 *
 * [흐름 — 2단계]
 *  1. sendCode() : 이메일로 회원을 찾아 코드 발송
 *  2. reset()    : 이메일 + 코드 + 새 비밀번호를 한 번에 제출 → 검증과 동시에 변경
 *
 * ★ resetToken 단계를 두지 않은 이유
 *   코드 확인과 새 비밀번호 입력을 프론트 한 화면에서 처리하기로 했기 때문.
 *   verified 와 used_at 이 항상 같은 시점에 세팅되므로 재사용 방지는
 *   verified=false 조건으로 충분히 보장된다.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long CODE_TTL_MINUTES = 5;

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetRepository passwordResetRepository,
                                VerificationCodeGenerator codeGenerator,
                                EmailSender emailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================================================================
    // 1) 인증코드 발송
    // ==================================================================

    /**
     * ★ 계정 열거 공격 방지
     *   해당 이메일로 가입된 회원이 없거나, 소셜 전용 계정(비밀번호가 없는 회원)이면
     *   조용히 무시하고 동일한 성공 응답을 준다.
     */
    @Transactional
    public VerificationResultResponse sendCode(PasswordResetSendCodeRequest request) {

        String email = normalize(request.email());

        userRepository.findByEmail(email)
                .filter(user -> !user.isSocialUser() && user.getPassword() != null)
                .ifPresentOrElse(
                        this::issueAndSend,
                        () -> log.info("비밀번호 재설정 요청 - 대상 없음 (조용히 무시)")
                );

        return VerificationResultResponse.ok("입력하신 이메일로 인증코드를 발송했습니다. (가입 정보가 없으면 발송되지 않습니다)");
    }

    private void issueAndSend(User user) {
        String code = codeGenerator.generate6Digit();
        PasswordReset reset = PasswordReset.create(user, user.getEmail(), code, CODE_TTL_MINUTES);
        passwordResetRepository.save(reset);

        String subject = "[LawChat] 비밀번호 재설정 인증코드";
        String body = "인증코드: " + code + " (5분 이내에 입력해 주세요)";
        emailSender.send(user.getEmail(), subject, body);
    }

    // ==================================================================
    // 2) 인증코드 확인 + 비밀번호 변경 (단일 단계)
    // ==================================================================

    @Transactional
    public void reset(PasswordResetRequest request) {

        String email = normalize(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        PasswordReset reset = passwordResetRepository
                .findFirstByUser_UserIdAndAuthTargetAndVerifiedFalseOrderByCreatedAtDesc(
                        user.getUserId(), email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        if (!reset.isUsable()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!reset.matches(request.code())) {
            reset.increaseAttempt();
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        reset.markVerifiedAndUsed();

        log.info("비밀번호 재설정 완료 - userId={}", user.getUserId());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}

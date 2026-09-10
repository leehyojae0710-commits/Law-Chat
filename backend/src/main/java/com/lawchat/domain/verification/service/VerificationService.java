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

    /**
     * 아이디 찾기용 인증코드 발송.
     *
     * 가입 이력이 있을 때만 실제로 보낸다. 없으면 조용히 무시하고 성공으로 응답한다.
     * "가입 정보가 없다" 고 알려주면 연락처를 넣어보는 것만으로 가입 여부를 알 수 있다.
     */
    @Transactional
    public VerificationResultResponse sendCode(SendCodeRequest request) {
        return sendCode(request, true, "아이디 찾기");
    }

    /**
     * 인증코드 발송.
     *
     * ★ requireExistingUser 를 나눈 이유
     *   아이디 찾기는 **가입된 사람만** 대상이다. 없는 연락처에 보내면
     *   번호를 넣어보는 것만으로 가입 여부를 알아낼 수 있어 조용히 무시한다.
     *
     *   회원가입 인증은 정반대다. 아직 가입하지 않은 번호가 본인 것인지 확인하는 절차라
     *   **DB 에 없어도 보내야 한다.** 기존 조건을 그대로 두면 신규 가입자는
     *   코드를 영영 받지 못한다.
     *
     *   같은 저장소(id_verifications)와 발송기를 쓰되 이 조건만 다르게 한다.
     *
     * @param requireExistingUser true 면 가입된 연락처에만 발송 (아이디 찾기),
     *                            false 면 가입 여부와 무관하게 발송 (회원가입 인증)
     */
    @Transactional
    public VerificationResultResponse sendCode(SendCodeRequest request, boolean requireExistingUser) {
        return sendCode(request, requireExistingUser, "아이디 찾기");
    }

    /**
     * 인증코드 발송 (용도 지정).
     *
     * purposeLabel 은 메일 제목에 들어간다.
     *   "아이디 찾기" / "계정 복구" / "회원가입"
     * 같은 절차를 여러 기능이 쓰는데 제목이 하나로 고정돼 있으면,
     * 복구하려는 사람이 "아이디 찾기" 메일을 받아 잘못 온 것으로 오해한다.
     */
    @Transactional
    public VerificationResultResponse sendCode(SendCodeRequest request, boolean requireExistingUser,
                                               String purposeLabel) {
        String normalizedValue = normalize(request.contactType(), request.contactValue());

        if (!requireExistingUser) {
            issueAndSend(request.contactType(), normalizedValue, purposeLabel);
            return VerificationResultResponse.ok("입력하신 연락처로 인증코드를 발송했습니다.");
        }

        findUserByContact(request.contactType(), normalizedValue)
                .ifPresentOrElse(
                        user -> issueAndSend(request.contactType(), normalizedValue, purposeLabel),
                        () -> log.info("아이디 찾기 요청 - 존재하지 않는 연락처 (조용히 무시): type={}", request.contactType())
                );

        return VerificationResultResponse.ok("입력하신 연락처로 인증코드를 발송했습니다. (가입 정보가 없으면 발송되지 않습니다)");
    }

    /**
     * 인증코드를 만들어 저장하고 발송한다.
     *
     * ★ 제목을 파라미터로 받는 이유
     *   예전에는 "[LawChat] 아이디 찾기 인증코드" 로 고정돼 있었다.
     *   같은 인증 절차를 계정 복구·회원가입에도 쓰는데, 복구하려는 사람이
     *   "아이디 찾기" 메일을 받으면 잘못 온 것으로 오해한다.
     *
     *   DB 에는 제목·본문을 저장하지 않으므로(코드와 연락처만 저장) 컬럼을 늘릴 필요가 없다.
     *   부르는 쪽이 용도에 맞는 문구를 넘기면 된다.
     *
     * @param purposeLabel 메일 제목에 들어갈 용도 (예: "아이디 찾기", "계정 복구", "회원가입")
     */
    private void issueAndSend(ContactType contactType, String contactValue, String purposeLabel) {
        String code = codeGenerator.generate6Digit();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);

        // id_verification 테이블 스키마 매핑
        IdVerification verification = IdVerification.builder()
                .authTarget(contactValue)
                .authCode(code)
                .expiredAt(expiredAt)
                .build();
        idVerificationRepository.save(verification);

        String subject = "[LawChat] " + purposeLabel + " 인증코드";
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

    /**
     * 회원가입 인증코드 확인.
     *
     * ★ 아이디 찾기와 나눈 이유
     *   아이디 찾기는 확인이 끝나면 **그 사람의 이메일을 돌려준다.**
     *   그래서 마지막에 사용자를 조회하는데, 가입 인증에서는 그 사용자가 아직 없어
     *   USER_NOT_FOUND 가 난다.
     *
     *   코드 검증 자체는 똑같으므로 그 부분만 함께 쓰고, 뒤처리를 나눈다.
     *   여기서는 검증 흔적만 남기고 아무것도 돌려주지 않는다.
     *   (가입 요청 시 그 흔적이 최근 것인지 확인한다)
     */
    @Transactional
    public void verifySignupCode(VerifyCodeRequest request) {
        String normalizedValue = normalize(request.contactType(), request.contactValue());
        consumeCode(normalizedValue, request.code());
        log.info("회원가입 인증 성공 - type={}", request.contactType());
    }

    /**
     * 인증코드를 대조하고 사용 처리한다.
     *
     * 성공하면 is_verified=true, used_at=now 가 기록된다.
     * 실패 사유(없음/만료/불일치)를 구분해 던져 화면이 다른 안내를 할 수 있게 한다.
     */
    private IdVerification consumeCode(String normalizedValue, String code) {
        IdVerification verification = idVerificationRepository
                .findFirstByAuthTargetAndIsVerifiedFalseOrderByCreatedAtDesc(normalizedValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        if (LocalDateTime.now().isAfter(verification.getExpiredAt())
                || verification.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!verification.getAuthCode().equals(code)) {
            verification.increaseAttempt();
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        verification.verify();
        return verification;
    }

    /**
     * 아이디 찾기 인증코드 확인.
     *
     * ★ @Transactional 이 반드시 있어야 한다
     *   이 클래스는 @Transactional(readOnly = true) 라, 붙이지 않으면 읽기 전용으로 돈다.
     *   그러면 consumeCode 안의 verification.verify() 로 is_verified=true, used_at 을 찍어도
     *   **DB 에 반영되지 않는다.**
     *   화면에서는 인증이 성공한 것처럼 보이지만 흔적이 남지 않아,
     *   뒤이은 계정 복구가 "전화번호 인증이 필요해요" 로 막힌다.
     */
    @Transactional
    public FindIdResultResponse verifyCode(VerifyCodeRequest request) {
        String normalizedValue = normalize(request.contactType(), request.contactValue());

        // 코드 대조·만료·시도횟수 확인과 사용 처리는 consumeCode 가 함께 맡는다.
        //   회원가입 인증과 똑같은 절차라 한 곳에 둔다. 두 벌로 두면 한쪽만 고쳐진다.
        consumeCode(normalizedValue, request.code());

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
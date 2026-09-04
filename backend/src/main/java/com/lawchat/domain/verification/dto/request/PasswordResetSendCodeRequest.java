package com.lawchat.domain.verification.dto.request;

import com.lawchat.domain.verification.entity.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 비밀번호 재설정 — 1단계: 인증코드 발송 요청.
 *
 * POST /api/verification/password/send-code
 *
 * EMAIL/PHONE(SMS) 둘 다 지원.
 *
 * @param contactType  EMAIL 또는 PHONE. 어느 컬럼으로 조회/발송할지 결정한다.
 * @param contactValue contactType 이 EMAIL 이면 이메일 주소, PHONE 이면 전화번호.
 */
public record PasswordResetSendCodeRequest(

        @NotNull(message = "인증 방식(EMAIL/PHONE)을 선택해 주세요.")
        ContactType contactType,

        @NotBlank(message = "이메일 또는 전화번호를 입력해 주세요.")
        String contactValue
) {
}
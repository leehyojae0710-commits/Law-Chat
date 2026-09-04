package com.lawchat.domain.verification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 — 1단계: 인증코드 발송 요청.
 *
 * POST /api/verification/password/send-code
 *
 * 지금은 이메일 전용. 전화번호는 User 테이블에 phone 컬럼이 추가되는 대로
 * contactType 필드를 다시 붙여 확장할 예정.
 */
public record PasswordResetSendCodeRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}

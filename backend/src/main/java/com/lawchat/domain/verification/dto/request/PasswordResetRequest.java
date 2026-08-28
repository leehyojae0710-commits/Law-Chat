package com.lawchat.domain.verification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 — 2단계(마지막 단계): 코드 확인 + 실제 비밀번호 변경을 한 번에 처리.
 *
 * POST /api/verification/password/reset
 */
public record PasswordResetRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증코드를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 숫자 6자리입니다.")
        String code,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다."
        )
        String newPassword
) {
}

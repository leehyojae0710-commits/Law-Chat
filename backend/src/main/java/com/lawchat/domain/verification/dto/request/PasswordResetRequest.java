package com.lawchat.domain.verification.dto.request;

import com.lawchat.domain.verification.entity.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 — 2단계(마지막 단계): 코드 확인 + 실제 비밀번호 변경을 한 번에 처리.
 *
 * POST /api/verification/password/reset
 *
 * EMAIL/PHONE(SMS) 둘 다 지원.
 */
public record PasswordResetRequest(

        @NotNull(message = "인증 방식(EMAIL/PHONE)을 선택해 주세요.")
        ContactType contactType,

        @NotBlank(message = "이메일 또는 전화번호를 입력해 주세요.")
        String contactValue,

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
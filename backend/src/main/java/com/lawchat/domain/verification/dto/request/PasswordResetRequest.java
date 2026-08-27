package com.lawchat.domain.verification.dto.request;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
 
/**
 * 비밀번호 재설정 — 3단계: 실제 비밀번호 변경 요청.
 *
 * POST /api/verification/password/reset
 *
 * @param resetToken  2단계(verify-code)에서 발급받은 1회용 토큰.
 * @param newPassword 새 비밀번호. SignupRequest 와 동일한 정책(8~64자, 영문+숫자)을 적용한다.
 */
public record PasswordResetRequest(
 
        @NotBlank(message = "인증 토큰이 필요합니다.")
        String resetToken,
 
        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다."
        )
        String newPassword
) {
}
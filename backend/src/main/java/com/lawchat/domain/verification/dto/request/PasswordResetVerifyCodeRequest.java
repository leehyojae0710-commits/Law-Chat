package com.lawchat.domain.verification.dto.request;
 
import com.lawchat.domain.verification.entity.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
 
/**
 * 비밀번호 재설정 — 2단계: 인증코드 확인 요청.
 *
 * POST /api/verification/password/verify-code
 * 성공하면 응답으로 resetToken 을 받고, 그 토큰으로 3단계(실제 재설정)를 호출한다.
 */
public record PasswordResetVerifyCodeRequest(
 
        @NotBlank(message = "이메일(아이디)을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
 
        @NotNull(message = "인증 방식(EMAIL/PHONE)을 선택해 주세요.")
        ContactType contactType,
 
        @NotBlank(message = "이메일 또는 전화번호를 입력해 주세요.")
        String contactValue,
 
        @NotBlank(message = "인증코드를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 숫자 6자리입니다.")
        String code
) {
}
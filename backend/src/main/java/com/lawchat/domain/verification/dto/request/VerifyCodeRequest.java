package com.lawchat.domain.verification.dto.request;
 
import com.lawchat.domain.verification.entity.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
 
/**
 * 아이디(이메일) 찾기 — 인증코드 확인 요청.
 *
 * POST /api/verification/id/verify-code
 */
public record VerifyCodeRequest(
 
        @NotNull(message = "인증 방식(EMAIL/PHONE)을 선택해 주세요.")
        ContactType contactType,
 
        @NotBlank(message = "이메일 또는 전화번호를 입력해 주세요.")
        String contactValue,
 
        @NotBlank(message = "인증코드를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 숫자 6자리입니다.")
        String code
) {
}
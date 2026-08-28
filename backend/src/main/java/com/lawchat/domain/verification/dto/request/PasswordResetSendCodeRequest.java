package com.lawchat.domain.verification.dto.request;
 
import com.lawchat.domain.verification.entity.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
 
/**
 * 비밀번호 재설정 — 1단계: 인증코드 발송 요청.
 *
 * POST /api/verification/password/send-code
 *
 * 아이디 찾기와 달리 email(로그인 아이디)을 반드시 같이 받는다.
 * 서버는 이 email 로 가입된 회원의 email/phone 이 contactValue 와 실제로
 * 일치하는지까지 확인한 뒤에만 코드를 발송한다(타인 계정 노림수 방지).
 */
public record PasswordResetSendCodeRequest(
 
        @NotBlank(message = "이메일(아이디)을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
 
        @NotNull(message = "인증 방식(EMAIL/PHONE)을 선택해 주세요.")
        ContactType contactType,
 
        @NotBlank(message = "이메일 또는 전화번호를 입력해 주세요.")
        String contactValue
) {
}
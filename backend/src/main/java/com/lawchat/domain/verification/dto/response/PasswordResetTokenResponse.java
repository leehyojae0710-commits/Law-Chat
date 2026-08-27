package com.lawchat.domain.verification.dto.response;
 
/**
 * 비밀번호 재설정 — 코드 확인 성공 시 응답.
 *
 * 프론트는 이 resetToken 을 들고 있다가 다음 화면(새 비밀번호 입력)에서
 * POST /api/verification/password/reset 요청 바디에 그대로 실어 보내면 된다.
 *
 * @param expiresInSeconds 이 토큰으로 실제 비밀번호 변경을 완료해야 하는 남은 시간(초).
 */
public record PasswordResetTokenResponse(
        String resetToken,
        long expiresInSeconds
) {
}
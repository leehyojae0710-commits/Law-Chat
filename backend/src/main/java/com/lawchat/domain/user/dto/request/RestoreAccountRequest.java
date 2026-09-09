package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 탈퇴 계정 복구 요청.
 *
 * 인증을 마친 연락처(이메일 또는 전화번호)만 받는다.
 * 인증코드를 다시 받지 않는 이유 — 기존 아이디 찾기 인증
 * (/api/verifications/verify-code)이 이미 검증 흔적을 남겨 뒀다.
 */
public record RestoreAccountRequest(

        @NotBlank(message = "인증한 이메일 또는 전화번호를 입력해 주세요.")
        String contactValue
) {
}

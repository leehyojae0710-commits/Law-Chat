package com.lawchat.domain.verification.dto.response;
 
/**
 * 아이디 찾기 — 인증코드 확인 성공 시 응답.
 *
 * 이 시점에는 이미 본인이 그 연락처(이메일/전화번호)의 소유자임이 코드로 증명되었으므로,
 * 로그인 아이디(이메일)를 마스킹 없이 그대로 돌려준다.
 */
public record FindIdResultResponse(
        String email
) {
    public static FindIdResultResponse of(String email) {
        return new FindIdResultResponse(email);
    }
}
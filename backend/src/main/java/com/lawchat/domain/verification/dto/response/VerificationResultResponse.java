package com.lawchat.domain.verification.dto.response;

/**
 * 인증코드 발송(send-code) API 의 공통 응답.
 *
 * ★ 계정 존재 여부를 응답으로 노출하지 않는다.
 *   해당 이메일로 가입된 회원이 없어도 항상 이 응답(success=true)을 그대로 내려준다.
 *   그래야 "이 이메일로 가입된 계정이 있는지"를 외부에서 API 응답만으로 알아낼 수 없다
 *   (계정 열거 공격 방지). 실제로 코드가 발송됐는지는 서버 로그로만 확인 가능.
 */
public record VerificationResultResponse(
        boolean success,
        String message
) {
    public static VerificationResultResponse ok(String message) {
        return new VerificationResultResponse(true, message);
    }
}

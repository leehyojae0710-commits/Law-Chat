package com.lawchat.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드.
 *
 * 에러를 enum 한 곳에 모아두면
 *  - 프론트가 code 문자열로 분기할 수 있고(메시지 문구가 바뀌어도 안전),
 *  - HTTP 상태코드를 실수로 뒤섞어 쓰는 일이 줄어든다.
 */
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    SOCIAL_USER_CANNOT_LOGIN_LOCALLY(HttpStatus.BAD_REQUEST, "소셜 계정으로 가입된 회원입니다. 소셜 로그인을 이용해 주세요."),

    // 401
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 로그인해 주세요."),

    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),

    // 404
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    PRECEDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "판례를 찾을 수 없습니다."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    EMAIL_ALREADY_REGISTERED_LOCALLY(HttpStatus.CONFLICT,
            "해당 이메일로 이미 가입된 계정이 있습니다. 이메일 로그인을 이용해 주세요."),

    // 502 — 외부 연동 실패
    KAKAO_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "카카오 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}

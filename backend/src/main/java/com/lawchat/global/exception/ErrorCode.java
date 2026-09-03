package com.lawchat.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드.
 */
public enum ErrorCode {

    // 400 - BAD REQUEST
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    SOCIAL_USER_CANNOT_LOGIN_LOCALLY(HttpStatus.BAD_REQUEST, "소셜 계정으로 가입된 회원입니다. 소셜 로그인을 이용해 주세요."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "로그인 요청이 유효하지 않습니다. 다시 시도해 주세요."),
    VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 요청을 찾을 수 없습니다. 인증코드를 다시 요청해 주세요."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었거나 시도 횟수를 초과했습니다. 다시 요청해 주세요."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 요청입니다. 처음부터 다시 시도해 주세요."),

    // 401 - UNAUTHORIZED
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다. 다시 로그인해 주세요."),
    SESSION_INVALIDATED(HttpStatus.UNAUTHORIZED, "다른 기기에서 로그인되어 로그아웃되었습니다."),

    // 403 - FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),

    // 404 - NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    PRECEDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "판례를 찾을 수 없습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공지사항입니다."),
    POPUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 팝업입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),

    // 409 - CONFLICT
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),
    EMAIL_ALREADY_REGISTERED_LOCALLY(HttpStatus.CONFLICT,
            "해당 이메일로 이미 가입된 계정이 있습니다. 이메일 로그인을 이용해 주세요."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변이 등록된 문의는 삭제할 수 없습니다."),

    // 502 - 외부 연동 실패
    KAKAO_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "카카오 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    NAVER_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "네이버 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    EMAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    PRECEDENT_AI_SUMMARY_FAILED(HttpStatus.BAD_GATEWAY, "AI 판례요약에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    // 400 - notice 도메인 추가
    INVALID_POPUP_PERIOD(HttpStatus.BAD_REQUEST, "노출 종료 일시는 시작 일시보다 뒤여야 합니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다."),

    // 500
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다. 잠시 후 다시 시도해주세요."),
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

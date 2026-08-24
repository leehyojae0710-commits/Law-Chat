package com.lawchat.global.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 모든 에러 응답의 공통 포맷.
 * 프론트는 code 값으로 분기하고, message 는 사용자에게 그대로 보여주면 된다.
 * fieldErrors 는 @Valid 검증 실패 시에만 채워진다. (필드명 -> 사유)
 */
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode.name(), message, fieldErrors, LocalDateTime.now());
    }
}

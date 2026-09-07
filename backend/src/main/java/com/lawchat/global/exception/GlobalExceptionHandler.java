package com.lawchat.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기.
 *
 * @RestControllerAdvice 를 붙이면 모든 @RestController 에서 던져진 예외를
 * 여기서 가로채 공통 포맷(ErrorResponse)으로 변환할 수 있다.
 * 덕분에 컨트롤러/서비스 코드에 try-catch 를 도배하지 않아도 된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 우리가 의도적으로 던진 비즈니스 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        // 의도된 예외이므로 스택트레이스까지 남기지 않고 warn 수준으로만 기록
        log.warn("[BusinessException] {} - {}", code.name(), e.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    /** @Valid 검증 실패 (DTO 필드 조건 위반) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            // 같은 필드에 검증이 여러 개면 첫 번째 메시지만 남긴다
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT,
                        ErrorCode.INVALID_INPUT.getMessage(), fieldErrors));
    }

    /**
     * multipart 요청이 아니거나 file 파트가 빠진 경우.
     *
     * ★ 별도 핸들러를 둔 이유
     *   이 예외는 클라이언트가 요청을 잘못 만든 것이라 400 계열이다.
     *   핸들러가 없으면 아래 handleUnexpected 로 떨어져 500 으로 응답하는데,
     *   그러면 프론트 문제인지 서버 문제인지 구분이 안 돼 디버깅이 크게 길어진다.
     */
    @ExceptionHandler({
            MultipartException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMultipart(Exception e) {
        log.warn("[MultipartException] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_FILE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_FILE,
                        "파일 업로드 형식이 올바르지 않습니다. multipart/form-data 로 file 파트를 보내주세요."));
    }

    /**
     * spring.servlet.multipart.max-file-size 초과.
     * ImageUploadValidator 의 5MB 검사보다 먼저 톰캣 단계에서 걸리는 경우다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("[MaxUploadSizeExceeded] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.FILE_TOO_LARGE.getStatus())
                .body(ErrorResponse.of(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    /** 그 외 예상하지 못한 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 예상 못 한 오류는 원인 파악이 필요하므로 스택트레이스까지 남긴다
        log.error("[UnexpectedException]", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                // 내부 예외 메시지를 그대로 노출하면 정보 유출이 되므로 고정 문구를 쓴다
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}

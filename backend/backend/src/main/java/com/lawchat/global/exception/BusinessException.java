package com.lawchat.global.exception;

/**
 * 비즈니스 규칙 위반을 표현하는 예외.
 *
 * RuntimeException(=unchecked)을 상속하는 이유:
 *  - checked 예외로 만들면 모든 호출부에 throws 가 전염된다.
 *  - 또한 Spring 의 @Transactional 은 기본적으로 "unchecked 예외"에서만 롤백한다.
 *    checked 로 만들면 롤백이 안 되어 잘못된 데이터가 커밋될 수 있다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.lawchat.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증이 필요한데 인증 정보가 없을 때(401) 실행되는 진입점.
 *
 * 기본 설정이면 Spring Security 가 HTML 로그인 페이지로 리다이렉트하거나
 * 빈 401 을 던지는데, REST API 서버에서는 항상 JSON 으로 응답하는 편이
 * 프론트에서 처리하기 편하다. 그래서 직접 구현해 등록한다.
 *
 * 참고: 이 클래스는 Security 필터 체인 안에서 동작하므로
 * @RestControllerAdvice(GlobalExceptionHandler)가 잡아주지 못한다.
 * (Advice 는 DispatcherServlet 안쪽, 즉 컨트롤러 영역의 예외만 처리한다)
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 필터가 request 에 담아둔 사유 코드를 꺼낸다.
     *
     * 프론트가 이 code 값으로 안내 문구를 분기할 수 있게 하기 위함이다.
     *   SESSION_INVALIDATED : 다른 기기에서 로그인되어 밀려남 -> "다른 기기에서 로그인되었습니다"
     *   INVALID_TOKEN       : 토큰이 없거나 만료/위조         -> "로그인이 필요합니다"
     */
    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object reason = request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_FAIL_REASON);
        if (ErrorCode.SESSION_INVALIDATED.name().equals(reason)) {
            return ErrorCode.SESSION_INVALIDATED;
        }
        return ErrorCode.INVALID_TOKEN;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // JwtAuthenticationFilter 가 남긴 차단 사유가 있으면 그 코드로 응답한다.
        // 없으면 기본값(INVALID_TOKEN = 그냥 로그인이 필요한 상황)을 쓴다.
        ErrorCode errorCode = resolveErrorCode(request);

        ErrorResponse body = ErrorResponse.of(errorCode, errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

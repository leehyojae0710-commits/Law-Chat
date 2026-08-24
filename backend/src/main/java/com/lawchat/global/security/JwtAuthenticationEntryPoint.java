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

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.of(
                ErrorCode.INVALID_TOKEN,
                ErrorCode.INVALID_TOKEN.getMessage()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

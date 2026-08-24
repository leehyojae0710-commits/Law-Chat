package com.lawchat.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 한 번씩 실행되며, Authorization 헤더의 JWT 를 읽어
 * "지금 이 요청을 보낸 사람이 누구인지"를 SecurityContext 에 등록하는 필터.
 *
 * [동작 순서]
 *  1. 헤더에서 "Bearer xxx" 형태의 토큰을 꺼낸다.
 *  2. 토큰이 유효하면 userId 와 권한을 담은 Authentication 객체를 만든다.
 *  3. SecurityContextHolder 에 넣어둔다.
 *     → 이후 컨트롤러에서 @AuthenticationPrincipal 로 userId 를 바로 받을 수 있다.
 *  4. 토큰이 없거나 잘못됐어도 여기서 예외를 던지지 않고 그냥 통과시킨다.
 *     인증이 필요한 URL 이면 뒤쪽 Security 필터가 401 을 내려주고,
 *     공개 URL 이면 비로그인 상태로 정상 처리되어야 하기 때문이다.
 *
 * OncePerRequestFilter 를 상속하는 이유:
 *  forward / include 등으로 같은 요청이 여러 번 필터를 타는 상황에서도
 *  딱 한 번만 실행되도록 보장해 준다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validate(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            boolean isAdmin = jwtTokenProvider.isAdmin(token);

            // 권한 문자열은 "ROLE_" 접두사 규칙을 따른다 (hasRole("ADMIN") 과 매칭됨)
            List<SimpleGrantedAuthority> authorities = isAdmin
                    ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));

            // 첫 번째 인자(principal)에 userId 를 넣었기 때문에
            // 컨트롤러에서 @AuthenticationPrincipal Long userId 로 받을 수 있다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 넘긴다. 이 호출을 빠뜨리면 요청이 여기서 멈춘다.
        filterChain.doFilter(request, response);
    }

    /** "Authorization: Bearer {token}" 헤더에서 토큰 부분만 잘라낸다. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

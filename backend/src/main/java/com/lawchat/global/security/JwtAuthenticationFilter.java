package com.lawchat.global.security;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 요청마다 한 번씩 실행되며, Authorization 헤더의 JWT 를 읽어
 * "지금 이 요청을 보낸 사람이 누구인지"를 SecurityContext 에 등록하는 필터.
 *
 * -------------------------------------------------------------
 * [동작 순서]
 *  1. 헤더에서 "Bearer xxx" 형태의 토큰을 꺼낸다.
 *  2. 서명/만료를 검증한다.                                    <- DB 안 봄 (빠름)
 *  3. * 토큰 안의 sessionToken 과 DB 의 users.session_token 을 대조한다. <- DB 조회 1회
 *  4. 일치하면 Authentication 을 만들어 SecurityContext 에 넣는다.
 *  5. 어느 단계든 실패하면 예외를 던지지 않고 그냥 통과시킨다.
 *     인증이 필요한 URL 이면 뒤쪽 Security 필터가 401 을 내려주고,
 *     공개 URL 이면 비로그인 상태로 정상 처리되어야 하기 때문이다.
 *
 * -------------------------------------------------------------
 * [3번이 이 설계의 핵심 - 왜 매 요청마다 DB 를 보는가]
 *
 *  JWT 는 원래 "서버가 아무것도 저장하지 않고 서명만 검증"하는 것이 장점이다.
 *  하지만 그 장점의 대가로, 한 번 발급된 토큰은 만료 전까지 서버가 취소할 수 없다.
 *  -> 로그아웃해도 토큰이 살아있고, 다른 기기에서 로그인해도 기존 기기가 계속 동작한다.
 *
 *  이 서비스는 "동시접속 차단"과 "로그아웃 즉시 반영"이 요구사항이므로,
 *  그 장점을 의도적으로 포기하고 매 요청마다 DB 를 한 번 조회하는 쪽을 택했다.
 *  조회 비용은 PK 단건 조회 하나뿐이라 매우 가볍다.
 *
 *  [대조가 실패하는 경우]
 *   - 다른 기기에서 로그인함 -> DB 의 session_token 이 새 값으로 바뀜 -> 불일치 -> 401
 *   - 로그아웃함             -> DB 의 session_token 이 null          -> 불일치 -> 401
 *   - 탈퇴함                 -> 마찬가지로 null                      -> 불일치 -> 401
 * -------------------------------------------------------------
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 차단 사유를 EntryPoint 로 전달하기 위한 request 속성 키.
     *
     * 필터는 401 을 직접 내려주지 않고(공개 URL 은 통과시켜야 하므로) 인증만 건너뛴다.
     * 실제 401 응답은 뒤쪽의 JwtAuthenticationEntryPoint 가 만드는데,
     * 거기서는 "왜" 인증이 없는지 알 수 없다. 그래서 사유를 여기에 담아 전달한다.
     *
     * 프론트는 이 코드로 안내 문구를 구분한다.
     *   SESSION_INVALIDATED -> "다른 기기에서 로그인되었습니다"
     *   INVALID_TOKEN       -> "로그인이 필요합니다"
     */
    public static final String ATTR_AUTH_FAIL_REASON = "authFailReason";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validate(token)) {
            authenticateIfSessionValid(token, request);
        }

        // 다음 필터로 넘긴다. 이 호출을 빠뜨리면 요청이 여기서 멈춘다.
        filterChain.doFilter(request, response);
    }

    /**
     * 토큰의 세션값이 DB 와 일치할 때만 인증 처리한다.
     *
     * 인증을 세팅하지 않고 그냥 빠져나가면, 인증이 필요한 URL 에서는
     * JwtAuthenticationEntryPoint 가 401 JSON 을 내려준다.
     */
    private void authenticateIfSessionValid(String token, HttpServletRequest request) {
        // 이 메서드에 들어왔다는 건 서명/만료 검증은 이미 통과했다는 뜻이다.
        // 따라서 여기서 실패하면 원인은 "세션 무효화"일 가능성이 높다.
        Long userId = jwtTokenProvider.getUserId(token);
        String tokenSessionValue = jwtTokenProvider.getSessionToken(token);

        // 구버전 토큰(sessionToken 클레임이 없던 시절에 발급된 것) 방어.
        // 이 값이 없으면 대조 자체가 불가능하므로 인증하지 않는다 -> 재로그인 유도.
        if (tokenSessionValue == null) {
            log.debug("세션 식별값이 없는 토큰 - userId={} (재로그인 필요)", userId);
            return;
        }

        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) {
            log.debug("존재하지 않는 사용자의 토큰 - userId={}", userId);
            return;
        }

        User user = found.get();

        // 탈퇴 회원은 토큰이 남아 있어도 차단한다.
        if (user.isDeleted()) {
            log.debug("탈퇴 회원의 토큰 - userId={}", userId);
            return;
        }

        // * 동시접속 차단의 실제 판정 지점
        if (!user.isSessionValid(tokenSessionValue)) {
            log.debug("무효화된 세션 - userId={} (다른 기기 로그인 또는 로그아웃됨)", userId);
            request.setAttribute(ATTR_AUTH_FAIL_REASON, ErrorCode.SESSION_INVALIDATED.name());
            return;
        }

        // 권한 문자열은 "ROLE_" 접두사 규칙을 따른다 (hasRole("ADMIN") 과 매칭됨)
        List<SimpleGrantedAuthority> authorities = Boolean.TRUE.equals(user.getIsAdmin())
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 첫 번째 인자(principal)에 userId 를 넣었기 때문에
        // 컨트롤러에서 @AuthenticationPrincipal Long userId 로 받을 수 있다.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
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

package com.lawchat.global.security;

import com.lawchat.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT(Json Web Token) 발급 / 검증 담당.
 *
 * [JWT 가 동작하는 원리]
 *  토큰은 점(.)으로 구분된 3조각이다 →  header.payload.signature
 *   - header    : 어떤 알고리즘으로 서명했는지 (예: HS256)
 *   - payload   : 실제 담긴 정보(여기서는 userId, isAdmin, 만료시각)
 *   - signature : header+payload 를 서버만 아는 secret 으로 해싱한 값
 *
 *  payload 는 단순 Base64 인코딩이라 누구나 열어볼 수 있다. → 비밀번호 같은 건 절대 넣지 않는다.
 *  대신 signature 덕분에 "내용을 위조하면 서명이 깨져서 바로 들통난다".
 *  그래서 서버는 세션을 메모리에 들고 있지 않아도 토큰만 검증해서 사용자를 신뢰할 수 있다(무상태).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** 토큰에 담을 커스텀 클레임 키 */
    private static final String CLAIM_IS_ADMIN = "isAdmin";
    private static final String CLAIM_NICKNAME = "nickname";
    /** 동시접속 차단용 세션 식별값. 매 요청마다 DB 의 users.session_token 과 대조된다. */
    private static final String CLAIM_SESSION_TOKEN = "sessionToken";

    private final SecretKey secretKey;
    private final long expirationMillis;

    /**
     * @param secret application.yml 의 jwt.secret 값.
     *               HS256 은 최소 256bit(=32byte) 이상의 키를 요구하므로
     *               한글 없이 영문/숫자 기준 32자 이상으로 설정해야 한다.
     * @param expirationMillis 토큰 유효시간(밀리초)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-millis}") long expirationMillis) {

        // 문자열 secret 을 HMAC-SHA 서명용 키 객체로 변환
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * 로그인 성공 시 액세스 토큰 생성.
     * subject 에는 PK(userId)를 넣는다. 이메일을 넣으면 이메일 변경 시 토큰이 꼬인다.
     */
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))            // sub
                .claim(CLAIM_IS_ADMIN, user.getIsAdmin())             // 권한 판단용
                .claim(CLAIM_NICKNAME, user.getNickname())            // 화면 표시 편의용
                .claim(CLAIM_SESSION_TOKEN, user.getSessionToken())   // ★ 동시접속 차단용
                .issuedAt(now)                                        // iat
                .expiration(expiry)                                   // exp
                .signWith(secretKey)                                  // 서명
                .compact();                                           // 문자열로 직렬화
    }

    /** 토큰 유효시간(초). 프론트에 만료까지 남은 시간을 알려줄 때 사용. */
    public long getExpiresInSeconds() {
        return expirationMillis / 1000;
    }

    /**
     * 토큰이 유효한지 검사한다.
     * 서명 불일치, 만료, 형식 오류 등은 모두 JwtException 계열로 던져진다.
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 만료/위조는 흔히 발생하므로 debug 수준으로만 남긴다
            log.debug("유효하지 않은 JWT: {}", e.getMessage());
            return false;
        }
    }

    /** 토큰에서 userId 추출 */
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 세션 식별값 추출.
     *
     * JwtAuthenticationFilter 가 이 값을 꺼내서 DB 의 users.session_token 과 대조한다.
     * 값이 다르면 = 다른 기기에서 새로 로그인했거나 로그아웃한 것이므로 인증을 거부한다.
     */
    public String getSessionToken(String token) {
        return parseClaims(token).get(CLAIM_SESSION_TOKEN, String.class);
    }

    /** 토큰에서 관리자 여부 추출 */
    public boolean isAdmin(String token) {
        Boolean value = parseClaims(token).get(CLAIM_IS_ADMIN, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    /**
     * 서명을 검증하면서 payload(Claims)를 꺼낸다.
     * verifyWith 로 키를 지정하지 않으면 서명 검증 없이 파싱돼서 위조를 잡을 수 없다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

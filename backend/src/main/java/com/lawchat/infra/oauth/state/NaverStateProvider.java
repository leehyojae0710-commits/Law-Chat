package com.lawchat.infra.oauth.state;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * 네이버 로그인의 CSRF 방지용 state 파라미터를 세션/DB/Redis 없이 발급·검증한다.
 *
 * [왜 무상태(stateless)로 만들었는가]
 *  state 의 목적은 "이 콜백이 우리가 방금 시작한 로그인 흐름이 맞는지" 확인하는 것뿐이다.
 *  세션에 저장해도 되지만, 이 프로젝트는 STATELESS(JWT) 로 설계돼 있어(SecurityConfig 참고)
 *  세션을 쓰려면 별도로 세션 저장소를 켜야 하고, Redis 를 쓰려면 인프라를 새로 붙여야 한다.
 *  대신 HMAC 서명값 자체를 state 로 사용하면, 검증 시점에 "서버가 발급한 값과 위변조 없이
 *  일치하는지"를 서명 재계산만으로 확인할 수 있어 저장소가 필요 없다.
 *
 * [형식]  base64url(timestamp:nonce) + "." + base64url(HMAC-SHA256(위 값))
 *  - timestamp : 발급 시각(ms). 검증 시 TTL_MILLIS 이내인지 확인한다.
 *  - nonce     : 같은 밀리초에 여러 번 발급해도 state 값이 겹치지 않도록.
 *  - signature : jwt.secret 으로 서명. 별도 설정값을 새로 추가하지 않기 위해
 *                이미 있는 JwtTokenProvider 의 secret 을 재사용한다.
 *                (서명 용도만 같을 뿐 JWT 토큰 자체와는 무관하다)
 *
 * ★ 한계 — 알고 넘어갈 것
 *  DB/Redis 에 "이미 쓴 state" 를 남기지 않으므로, TTL(5분) 이내라면 동일한 state 를
 *  재전송해도 서명 검증만으로는 막지 못한다(재사용 방지가 아니라 위변조 방지 용도).
 *  네이버 인가코드 자체가 1회용이라 실질 피해로 이어지긴 어렵지만, 완전한 재사용 방지가
 *  필요해지면 Redis 로 옮기고 SETNX 로 "이미 검증된 state" 를 표시하는 방식을 권장한다.
 */
@Component
public class NaverStateProvider {

    private static final long TTL_MILLIS = 5 * 60 * 1000L; // 5분 — 로그인 화면에서 지체돼도 통과할 정도의 여유
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec signingKey;

    /** jwt.secret 을 재사용한다 — 이 값 전용 설정을 yml 에 새로 추가하지 않기 위함. */
    public NaverStateProvider(@Value("${jwt.secret}") String secret) {
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    /** 로그인 시작 시 발급. 프론트는 이 값을 네이버 인가 URL 의 state 파라미터에 그대로 실어 보낸다. */
    public String generate() {
        String payload = System.currentTimeMillis() + ":" + UUID.randomUUID();
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    /**
     * 콜백에서 돌아온 state 를 검증한다.
     * 형식이 다르거나, 서명이 위조됐거나, 발급 후 TTL_MILLIS 가 지났으면 예외를 던진다.
     */
    public void validate(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        String[] parts = state.split("\\.", 2);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        String encodedPayload = parts[0];
        String signature = parts[1];

        // 서명 비교는 타이밍 공격을 피하기 위해 상수 시간 비교(MessageDigest.isEqual)를 쓴다.
        String expectedSignature = sign(encodedPayload);
        if (!MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }

        long issuedAt = parseTimestamp(encodedPayload);
        if (System.currentTimeMillis() - issuedAt > TTL_MILLIS) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
    }

    private long parseTimestamp(String encodedPayload) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            return Long.parseLong(payload.split(":", 2)[0]);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            byte[] hash = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            return encode(hash);
        } catch (java.security.GeneralSecurityException e) {
            // HmacSHA256 은 표준 JVM 이 항상 지원하므로 정상 동작 중엔 발생하지 않는다.
            throw new IllegalStateException("state 서명에 실패했습니다.", e);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

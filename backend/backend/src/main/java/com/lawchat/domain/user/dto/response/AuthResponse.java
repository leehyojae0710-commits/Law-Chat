package com.lawchat.domain.user.dto.response;

import com.lawchat.domain.user.entity.User;

/**
 * 로그인 / 회원가입 성공 시 응답.
 *
 * accessToken 을 클라이언트에 내려주고, 이후 요청부터는
 * Authorization: Bearer {accessToken} 헤더에 실어 보내면 된다.
 *
 * tokenType 을 함께 내려주는 이유는 프론트가 헤더를 조립할 때
 * 접두사를 하드코딩하지 않아도 되게 하기 위함이다.
 */
public record AuthResponse(
        String tokenType,     // 항상 "Bearer"
        String accessToken,
        long expiresIn,       // 만료까지 남은 초(second)
        UserProfileResponse user
) {
    public static AuthResponse of(String accessToken, long expiresInSeconds, User user) {
        return new AuthResponse(
                "Bearer",
                accessToken,
                expiresInSeconds,
                UserProfileResponse.from(user)
        );
    }
}

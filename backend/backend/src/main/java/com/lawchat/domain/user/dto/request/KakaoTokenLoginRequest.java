package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * [방식 B] 액세스 토큰 방식 카카오 로그인 요청.
 *
 * 프론트가 Kakao SDK for JavaScript 로 직접 로그인해 액세스 토큰까지 받은 뒤
 * 그 토큰만 서버로 보내는 경우에 쓴다.
 *
 * ★ 이 경우 서버는 "받은 토큰이 진짜인지 + 우리 앱 것인지"를 반드시 검증해야 한다.
 *   검증 없이 사용자 정보만 조회하면 남의 앱 토큰으로도 로그인이 뚫린다.
 *   → KakaoOAuthClient.verifyAccessToken() 참고
 */
public record KakaoTokenLoginRequest(

        @NotBlank(message = "카카오 액세스 토큰이 필요합니다.")
        String kakaoAccessToken
) {
}

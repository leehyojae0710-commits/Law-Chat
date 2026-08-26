package com.lawchat.infra.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET https://kapi.kakao.com/v1/user/access_token_info 응답.
 *
 * ★ 이 API가 바로 "토큰 검증" 전용 엔드포인트다.
 *   프론트(JS SDK)가 직접 로그인해서 받은 액세스 토큰을 서버로 보냈을 때,
 *   그 토큰이 (1) 살아있는지 (2) 정말 우리 앱에서 발급된 것인지 확인하는 데 쓴다.
 *
 *   (2)의 검증이 특히 중요하다. 공격자가 자기 앱에서 발급한 카카오 토큰을 보내면
 *   /v2/user/me 는 정상 응답하기 때문에, appId 대조 없이는 남의 앱 사용자로
 *   우리 서비스에 로그인할 수 있게 된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenInfo(

        /** 토큰의 주인인 카카오 회원번호 */
        @JsonProperty("id") Long id,

        /** 토큰이 발급된 앱의 ID → 우리 앱 ID와 반드시 대조할 것 */
        @JsonProperty("app_id") Long appId,

        /** 만료까지 남은 초 */
        @JsonProperty("expires_in") Integer expiresIn
) {
}

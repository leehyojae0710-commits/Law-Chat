package com.lawchat.infra.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST https://kauth.kakao.com/oauth/token 응답.
 *
 * 카카오 응답은 snake_case 이고 우리 자바 필드는 camelCase 이므로
 * @JsonProperty 로 이름을 명시적으로 이어준다.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) 를 붙이는 이유:
 *  카카오가 나중에 응답 필드를 추가해도 우리 쪽에서 파싱 예외가 나지 않게 하기 위함.
 *  (기본값이면 모르는 필드 발견 시 UnrecognizedPropertyException 이 터진다)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(

        /** 사용자 정보 조회 등에 쓸 액세스 토큰 */
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("token_type") String tokenType,

        /** 재발급용. 저장하려면 별도 테이블이 필요하다(현재 스키마에는 없음) */
        @JsonProperty("refresh_token") String refreshToken,

        /** 액세스 토큰 만료까지 남은 초 */
        @JsonProperty("expires_in") Integer expiresIn,

        /** 사용자가 동의한 항목 목록 (공백 구분) */
        @JsonProperty("scope") String scope
) {
}

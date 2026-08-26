package com.lawchat.infra.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET https://nid.naver.com/oauth2.0/token 응답.
 *
 * 네이버 응답도 카카오처럼 snake_case 이고, 실패 시에는 access_token 대신
 * error / error_description 필드가 내려온다 (HTTP 상태코드는 200으로 오는 경우가 있어
 * 반드시 error 필드 유무로 성공/실패를 구분해야 한다 — NaverOAuthClient 참고).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverTokenResponse(

        @JsonProperty("access_token") String accessToken,

        @JsonProperty("refresh_token") String refreshToken,

        @JsonProperty("token_type") String tokenType,

        /** 액세스 토큰 만료까지 남은 초 */
        @JsonProperty("expires_in") String expiresIn,

        /** 실패 시에만 내려옴. 예: "invalid_request" */
        @JsonProperty("error") String error,

        @JsonProperty("error_description") String errorDescription
) {
    public boolean isError() {
        return error != null && !error.isBlank();
    }
}

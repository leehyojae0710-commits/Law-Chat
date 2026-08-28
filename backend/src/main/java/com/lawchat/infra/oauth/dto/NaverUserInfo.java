package com.lawchat.infra.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET https://openapi.naver.com/v1/nid/me 응답.
 *
 * 카카오와 달리 실제 프로필 필드가 response 한 겹 안에 더 들어간다.
 * resultcode 가 "00" 이 아니면 실패이므로 NaverOAuthClient 에서 먼저 걸러낸다.
 *
 * ★ 이메일/닉네임/프로필사진은 네이버 개발자센터에서 "제공 항목"으로 설정하지 않으면
 *   응답에서 통째로 빠지거나 null 로 온다. 카카오와 마찬가지로 null 안전 접근이 필요하다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserInfo(

        @JsonProperty("resultcode") String resultCode,

        @JsonProperty("message") String message,

        @JsonProperty("response") NaverProfile response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverProfile(
            /** 네이버 회원번호. 앱 단위로 고유하며 변하지 않는다 → users.social_id 로 저장 */
            @JsonProperty("id") String id,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("profile_image") String profileImage,
            @JsonProperty("name") String name
    ) {
    }

    public boolean isSuccess() {
        return "00".equals(resultCode) && response != null;
    }

    // ---------- null 안전 접근용 편의 메서드 (카카오 KakaoUserInfo 와 동일한 패턴) ----------

    public String getIdOrNull() {
        return response == null ? null : response.id();
    }

    public String getNicknameOrNull() {
        return response == null ? null : response.nickname();
    }

    public String getProfileImageOrNull() {
        return response == null ? null : response.profileImage();
    }

    public String getEmailOrNull() {
        return response == null ? null : response.email();
    }
}

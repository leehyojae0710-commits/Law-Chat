package com.lawchat.infra.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET https://kapi.kakao.com/v2/user/me 응답.
 *
 * 카카오 응답이 중첩 구조라 내부 record 로 그대로 모양을 맞췄다.
 *
 * ★ 주의: 닉네임/프로필사진/이메일은 "동의항목"이라
 *   사용자가 동의하지 않으면 아예 응답에 없거나 null 이다.
 *   따라서 모든 접근에 null 체크가 필요하다. → 아래 편의 메서드로 감쌌다.
 *   특히 이메일은 [비즈니스 앱] 전환 후에야 동의항목으로 쓸 수 있어
 *   개발 단계에서는 거의 항상 null 이라고 보면 된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfo(

        /** 카카오 회원번호. 앱 단위로 고유하며 변하지 않는다 → users.social_id 로 저장 */
        @JsonProperty("id") Long id,

        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            @JsonProperty("email") String email,
            @JsonProperty("is_email_verified") Boolean isEmailVerified,
            @JsonProperty("profile") Profile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("nickname") String nickname,
            @JsonProperty("profile_image_url") String profileImageUrl
    ) {
    }

    // ---------- null 안전 접근용 편의 메서드 ----------

    public String getNicknameOrNull() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) return null;
        return kakaoAccount.profile().nickname();
    }

    public String getProfileImageOrNull() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) return null;
        return kakaoAccount.profile().profileImageUrl();
    }

    /** 인증된 이메일만 반환한다. 미동의/미인증이면 null */
    public String getVerifiedEmailOrNull() {
        if (kakaoAccount == null) return null;
        if (!Boolean.TRUE.equals(kakaoAccount.isEmailVerified())) return null;
        return kakaoAccount.email();
    }
}

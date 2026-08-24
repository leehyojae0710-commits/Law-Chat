package com.lawchat.infra.oauth.client;

import com.lawchat.infra.oauth.dto.KakaoTokenResponse;
import com.lawchat.infra.oauth.dto.KakaoUserInfo;
import com.lawchat.infra.oauth.config.KakaoOAuthProperties;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 인증 서버와 통신하는 전용 클라이언트.
 *
 * ★ 이 클래스만 카카오를 알고 있고, UserService 는 "검증된 결과"만 받아간다.
 *   나중에 네이버/구글을 추가할 때 UserService 를 건드리지 않아도 되도록 분리한 것.
 *
 * [카카오가 제공하는 서버는 두 개다]
 *   - kauth.kakao.com : 인증(토큰 발급/갱신/만료)
 *   - kapi.kakao.com  : API(사용자 정보 조회, 토큰 정보 조회, 로그아웃/연결끊기)
 */
@Component
public class KakaoOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(RestClient kakaoRestClient, KakaoOAuthProperties properties) {
        this.restClient = kakaoRestClient;
        this.properties = properties;
    }

    // ==================================================================
    // 1) 인가코드 → 액세스 토큰  (★ client_secret 이 사용되는 유일한 지점)
    // ==================================================================

    /**
     * 프론트가 카카오에서 받아 넘겨준 "인가코드(authorization code)"를
     * 실제 액세스 토큰으로 교환한다.
     *
     * ★ 왜 이 과정을 서버에서 해야 하는가 (= 왜 시크릿이 서버에만 있어야 하는가)
     *   인가코드는 URL 에 노출되기 때문에 그것만으로는 신뢰할 수 없다.
     *   "이 인가코드를 토큰으로 바꿀 자격이 있는 진짜 우리 서비스인가"를
     *   client_secret 으로 증명하는 것이다.
     *   프론트에 시크릿을 두면 누구나 개발자도구에서 꺼내 쓸 수 있어 의미가 사라진다.
     *
     * 요청 형식은 JSON 이 아니라 application/x-www-form-urlencoded 다. (카카오 스펙)
     * 인가코드는 1회용이며 유효시간이 매우 짧다 → 재사용하면 KOE320 에러.
     */
    public KakaoTokenResponse requestToken(String authorizationCode, String redirectUriOverride) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());          // REST API 키
        form.add("redirect_uri",
                redirectUriOverride != null ? redirectUriOverride : properties.redirectUri());
        form.add("code", authorizationCode);

        // 클라이언트 시크릿은 "사용함"으로 설정한 경우에만 넣는다.
        // 활성화해두고 안 보내면 KOE010 에러가 발생한다.
        if (properties.hasClientSecret()) {
            form.add("client_secret", properties.clientSecret());
        }

        try {
            return restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

        } catch (RestClientException e) {
            // ★ 예외 메시지에 form 내용을 찍으면 시크릿이 로그에 남는다. 절대 금지.
            log.warn("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    // ==================================================================
    // 2) 사용자 정보 조회
    // ==================================================================

    /**
     * 액세스 토큰으로 카카오 회원번호/닉네임/프로필사진/이메일을 가져온다.
     * 닉네임·이메일 등은 동의항목이라 사용자가 동의하지 않으면 비어 있을 수 있다.
     */
    public KakaoUserInfo requestUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfo userInfo = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfo.class);

            if (userInfo == null || userInfo.id() == null) {
                throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
            }
            return userInfo;

        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    // ==================================================================
    // 3) 연결 끊기 (회원 탈퇴 시 호출 권장)
    // ==================================================================

    /**
     * 우리 서비스와 카카오 계정의 연결을 해제한다.
     * 회원 탈퇴 시 호출하지 않으면 카카오 쪽에는 계속 연결된 앱으로 남는다.
     * 실패해도 우리 서비스의 탈퇴는 진행되어야 하므로 예외를 던지지 않고 로그만 남긴다.
     */
    public void unlink(String kakaoAccessToken) {
        try {
            restClient.post()
                    .uri(UNLINK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("카카오 연결 끊기 실패(무시하고 진행): {}", e.getMessage());
        }
    }
}

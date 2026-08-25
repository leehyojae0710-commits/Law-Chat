package com.lawchat.infra.oauth.client;

import com.lawchat.infra.oauth.config.NaverOAuthProperties;
import com.lawchat.infra.oauth.dto.NaverTokenResponse;
import com.lawchat.infra.oauth.dto.NaverUserInfo;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 네이버 인증 서버와 통신하는 전용 클라이언트.
 *
 * ★ 이 클래스만 네이버를 알고 있고, UserService 는 "검증된 결과"만 받아간다.
 *   KakaoOAuthClient 와 동일한 역할 분리다.
 *
 * [네이버가 제공하는 서버는 두 개다]
 *   - nid.naver.com     : 인증(토큰 발급)
 *   - openapi.naver.com : API(사용자 정보 조회)
 *
 * [카카오와 다른 점]
 *   - client_secret 이 항상 필수다 (카카오는 선택).
 *   - 토큰 요청에 state 가 필수다. redirect_uri 는 요청 파라미터로 받지 않는다
 *     (인가 요청 때 등록된 Callback URL 과 code 만으로 서버가 식별한다).
 *   - 사용자 정보가 response 한 겹 안에 더 들어간다.
 */
@Component
public class NaverOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthClient.class);

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient;
    private final NaverOAuthProperties properties;

    public NaverOAuthClient(RestClient naverRestClient, NaverOAuthProperties properties) {
        this.restClient = naverRestClient;
        this.properties = properties;
    }

    // ==================================================================
    // 1) 인가코드 → 액세스 토큰  (★ client_secret 이 사용되는 유일한 지점)
    // ==================================================================

    /**
     * 프론트가 네이버에서 받아 넘겨준 "인가코드(authorization code)"와 state 를
     * 실제 액세스 토큰으로 교환한다.
     *
     * state 검증(위변조/만료 확인)은 이 메서드를 호출하기 전에
     * UserService 에서 NaverStateProvider 로 이미 끝낸 상태여야 한다.
     * 여기서는 검증된 state 값을 네이버 토큰 API 요청 파라미터로 그대로 전달할 뿐이다.
     *
     * ★ redirectUriOverride 를 파라미터로 받아두긴 하지만 네이버 토큰 요청 자체에는
     *   보내지 않는다(네이버 스펙에 redirect_uri 파라미터가 없다). 프론트-백엔드 간
     *   요청 형식을 카카오와 동일하게 맞춰 향후 provider 를 늘릴 때 인터페이스가
     *   흔들리지 않게 하기 위해 시그니처만 맞춰둔 것이다.
     */
    public NaverTokenResponse requestToken(String authorizationCode, String state, String redirectUriOverride) {

        URI uri = UriComponentsBuilder.fromUriString(TOKEN_URL)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", properties.clientId())
                .queryParam("client_secret", properties.clientSecret())
                .queryParam("code", authorizationCode)
                .queryParam("state", state)
                .build()
                .toUri();

        try {
            NaverTokenResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NaverTokenResponse.class);

            // ★ 네이버는 실패해도 HTTP 200 으로 내려주고 error 필드로만 구분하는 경우가 있어
            //   상태코드만으로는 실패를 못 잡는다. 응답 바디를 반드시 확인해야 한다.
            if (response == null || response.accessToken() == null || response.isError()) {
                log.warn("네이버 토큰 발급 실패: {}",
                        response == null ? "응답 없음" : response.error() + " / " + response.errorDescription());
                throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
            }
            return response;

        } catch (RestClientException e) {
            // ★ 예외 메시지에 요청 내용을 찍으면 시크릿이 로그에 남는다. 절대 금지.
            log.warn("네이버 토큰 발급 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }
    }

    // ==================================================================
    // 2) 사용자 정보 조회
    // ==================================================================

    /**
     * 액세스 토큰으로 네이버 회원번호/닉네임/프로필사진/이메일을 가져온다.
     * 개발자센터에서 "제공 항목"으로 설정하지 않은 값은 비어 있을 수 있다.
     */
    public NaverUserInfo requestUserInfo(String naverAccessToken) {
        try {
            NaverUserInfo userInfo = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + naverAccessToken)
                    .retrieve()
                    .body(NaverUserInfo.class);

            if (userInfo == null || !userInfo.isSuccess()) {
                throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
            }
            return userInfo;

        } catch (RestClientException e) {
            log.warn("네이버 사용자 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }
    }
}

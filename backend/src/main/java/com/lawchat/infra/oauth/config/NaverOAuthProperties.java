package com.lawchat.infra.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 네이버 로그인 설정값 바인딩.
 *
 * application.yml 의 oauth.naver.* 항목이 이 record 필드에 자동으로 주입된다.
 * (LawChatApplication 에 @ConfigurationPropertiesScan 이 붙어 있어야 스캔된다)
 *
 * 카카오와의 차이: 네이버는 client-secret 이 처음부터 필수값이라
 * KakaoOAuthProperties.hasClientSecret() 같은 선택적 분기가 필요 없다.
 *
 * @param clientId     Client ID. 공개돼도 되는 앱 식별자
 * @param clientSecret Client Secret. ★서버만 알아야 하는 비밀값★ (토큰 교환 요청에만 사용)
 * @param redirectUri  네이버 개발자센터에 등록한 Callback URL 과 "문자 하나까지" 같아야 한다.
 *                     다르면 invalid_request 에러가 난다.
 */
@ConfigurationProperties(prefix = "oauth.naver")
public record NaverOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}

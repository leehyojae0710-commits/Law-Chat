package com.lawchat.infra.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 로그인 설정값 바인딩.
 *
 * application.yml 의 oauth.kakao.* 항목이 이 record 필드에 자동으로 주입된다.
 * (LawChatApplication 에 @ConfigurationPropertiesScan 이 붙어 있어야 스캔된다)
 *
 * ★★ 여기가 "키를 넣는 곳"의 실체다. ★★
 *    다만 값 자체는 이 파일이 아니라 application.yml → 환경변수로 흘러들어온다.
 *    소스에 직접 적으면 깃허브에 그대로 올라가므로 절대 하드코딩하지 말 것.
 *
 * @param clientId     REST API 키. 카카오 앱을 식별하는 공개 값 (프론트에 노출돼도 무방)
 * @param clientSecret 클라이언트 시크릿. ★서버만 알아야 하는 비밀값★
 *                     토큰 발급(= 인가코드 검증) 요청에만 사용된다.
 * @param redirectUri  카카오 개발자센터에 등록한 값과 "문자 하나까지" 같아야 한다.
 *                     다르면 KOE006 에러가 난다.
 * @param appId        내 앱의 고유 번호. 프론트가 보낸 액세스 토큰이
 *                     "정말 우리 앱에서 발급된 것인지" 검증할 때 대조한다.
 */
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        Long appId
) {
    /** 시크릿을 설정하지 않은 앱(비활성화한 경우)도 있으므로 존재 여부를 확인해 준다. */
    public boolean hasClientSecret() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}

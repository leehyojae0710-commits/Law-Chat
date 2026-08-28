package com.lawchat.domain.user.entity;

/**
 * 소셜 로그인 제공자.
 *
 * 이전에는 UserService 에 {@code private static final String KAKAO = "KAKAO";}
 * 형태로 문자열 상수를 두고 있었다. 네이버를 추가하면서 문자열을 하드코딩하면
 *  - 오타(KAKAO vs Kakao)를 컴파일 타임에 잡을 수 없고,
 *  - "지원하는 provider 목록"이 코드 어디에도 명시적으로 드러나지 않는다.
 * enum 으로 승격하면 두 문제 모두 해결되고, 향후 구글 등을 추가할 때도
 * 이 enum 에 한 줄만 추가하면 된다.
 *
 * DB 컬럼(users.social_provider)은 VARCHAR(50) 이고 값도 "KAKAO"/"NAVER" 문자열 그대로
 * 저장되므로, @Enumerated(EnumType.STRING) 매핑만 하면 스키마 변경은 필요 없다.
 */
public enum SocialProvider {
    KAKAO,
    NAVER
}

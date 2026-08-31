package com.lawchat.inquiry.controller;

import java.lang.annotation.*;

/**
 * 프로젝트에 이미 로그인 사용자 ID를 꺼내는 방식이 있다면 그것을 쓰세요.
 * 없다면 이 애노테이션 + ArgumentResolver 로 컨트롤러를 깔끔하게 유지할 수 있습니다.
 *
 * 이미 Spring Security 의 @AuthenticationPrincipal 커스텀 principal 을 쓰고 있다면
 * 아래 파라미터들을 전부 다음처럼 바꾸면 됩니다.
 *   @AuthenticationPrincipal CustomUserDetails principal  ->  principal.getUserId()
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthenticationPrincipalUserId {
}

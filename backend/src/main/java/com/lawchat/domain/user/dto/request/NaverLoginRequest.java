package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 네이버 로그인 — 인가코드 방식 요청.
 *
 * 카카오의 KakaoLoginRequest 와 거의 같지만 state 가 필수로 하나 더 있다.
 * 프론트는 로그인 시작 전에 GET /api/auth/naver/state 로 발급받은 state 를
 * 네이버 인가 URL 의 state 파라미터에 그대로 실어 보내고, 콜백에서 받은 값을
 * 여기로 다시 넘겨야 한다. 서버는 발급했던 값과 일치하는지 검증한다(CSRF 방지).
 *
 * @param code        네이버가 redirect_uri 쿼리로 내려준 인가코드. 1회용, 유효시간 짧음.
 * @param state        GET /api/auth/naver/state 로 발급받았던 값. 인가 요청 때 보낸 값과
 *                      정확히 같아야 한다.
 * @param redirectUri  프론트가 실제로 사용한 redirect_uri.
 *                     (로컬/운영 주소가 다를 수 있어 받아둔다. null 이면 서버 설정값 사용)
 *                     ★ 토큰 요청 시 인가코드를 받을 때 쓴 값과 완전히 동일해야 한다.
 */
public record NaverLoginRequest(

        @NotBlank(message = "인가코드가 필요합니다.")
        String code,

        @NotBlank(message = "state 값이 필요합니다.")
        String state,

        String redirectUri
) {
}

package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * [방식 A] 인가코드 방식 카카오 로그인 요청.
 *
 * 프론트는 카카오 로그인 화면을 거쳐 redirect_uri 로 받은 code 만 넘기면 된다.
 * 액세스 토큰 발급(= 인가코드 검증)은 전적으로 서버가 처리한다.
 *
 * @param code        카카오가 redirect_uri 쿼리로 내려준 인가코드. 1회용, 유효시간 짧음.
 * @param redirectUri 프론트가 실제로 사용한 redirect_uri.
 *                    (로컬/운영 주소가 다를 수 있어 받아둔다. null 이면 서버 설정값 사용)
 *                    ★ 토큰 요청 시 인가코드를 받을 때 쓴 값과 완전히 동일해야 한다.
 */
public record KakaoLoginRequest(

        @NotBlank(message = "인가코드가 필요합니다.")
        String code,

        String redirectUri
) {
}

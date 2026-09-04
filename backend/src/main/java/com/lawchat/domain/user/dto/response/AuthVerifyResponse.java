package com.lawchat.domain.user.dto.response;

import com.lawchat.domain.user.entity.User;

/**
 * 토큰 유효성 확인 응답 (GET /api/auth/verify 전용).
 *
 * ★ UserProfileResponse 와 따로 두는 이유
 *   이 API 의 목적은 "이 토큰이 아직 살아있는가"를 확인하는 것뿐이다.
 *   그런데 UserProfileResponse 를 재사용하면 email, profileImg, createdAt,
 *   socialProvider 까지 매번 함께 내려간다.
 *   앱이 켜질 때마다 호출되는 API 가 목적에 불필요한 개인정보를 계속 흘리는 셈이라,
 *   화면을 그리는 데 실제로 필요한 최소 3개 필드만 담는 전용 DTO 를 만들었다.
 *
 *   userId   : 프론트가 "누구로 로그인되어 있는지" 식별
 *   nickname : 헤더 등에 사용자 이름 표시
 *   isAdmin  : 관리자 메뉴 노출 여부 판단
 *
 *   그 외 상세 프로필이 필요하면 GET /api/users/me 를 쓰면 된다.
 *
 * ★ status 를 넣지 않은 이유
 *   이 응답이 200 으로 내려갔다는 것 자체가 "정상 로그인 상태"를 의미한다.
 *   (로그아웃/탈퇴/세션무효 상태였다면 필터에서 401 로 막혀 여기까지 오지 못한다)
 *   따라서 status 를 함께 내려주는 것은 중복이며, 프론트가 굳이 검사할 필요도 없다.
 */
public record AuthVerifyResponse(
        Long userId,
        String nickname,
        Boolean isAdmin
) {
    public static AuthVerifyResponse from(User user) {
        return new AuthVerifyResponse(
                user.getUserId(),
                user.getNickname(),
                user.getIsAdmin()
        );
    }
}

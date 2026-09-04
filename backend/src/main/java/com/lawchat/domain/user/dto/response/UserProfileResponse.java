package com.lawchat.domain.user.dto.response;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;
import com.lawchat.global.file.FileUrls;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답.
 *
 * ★ 엔티티(User)를 그대로 응답에 쓰지 않고 DTO 로 변환하는 이유
 *   1. password 같은 민감 정보가 JSON 에 섞여 나가는 사고를 원천 차단한다.
 *   2. 엔티티 필드명을 바꿔도 API 스펙이 깨지지 않는다(변경 격리).
 *   3. 지연 로딩 연관관계가 직렬화되며 예상 못 한 쿼리가 나가는 문제를 막는다.
 */
public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        String phone,
        String profileImg,
        String socialProvider,
        UserStatus status,
        Boolean isAdmin,
        LocalDateTime createdAt
) {
    /**
     * 엔티티 -> DTO 변환. password 는 의도적으로 포함하지 않는다.
     *
     * phone 은 프로필 수정 화면에서 "현재 등록된 번호"를 폼에 채워 보여줘야 하므로 포함한다.
     * DB 에는 숫자만 저장되므로(회원가입/수정 시 정규화) 하이픈 없이 내려간다.
     * 미등록 회원(소셜 가입 등)은 null 이다.
     *
     * socialProvider 는 엔티티에서 SocialProvider enum(User.getSocialProvider())으로 관리하지만,
     * 응답 JSON 모양은 기존과 동일하게 문자열로 유지한다(프론트 스펙 변경 없음).
     * 일반(이메일) 가입자는 소셜 provider 가 없으므로 null 로 내려간다.
     */
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getPhone(),
                toProfileImgUrl(user.getProfileImg()),
                user.getSocialProvider() == null ? null : user.getSocialProvider().name(),
                user.getStatus(),
                user.getIsAdmin(),
                user.getCreatedAt()
        );
    }

    /**
     * profile_img 컬럼에는 두 종류의 값이 섞여 들어온다.
     *
     *   1) 소셜 로그인 회원  : 카카오/네이버가 준 외부 절대 URL
     *                        예) https://k.kakaocdn.net/dn/.../profile.jpg
     *   2) 직접 업로드한 회원 : 우리 공유폴더에 저장된 파일명
     *                        예) 3f9c2e1a-..._profile.jpg
     *
     * 2번만 FileUrls.view() 로 절대 URL 을 만들어야 한다.
     * 1번까지 감싸면 "우리 서버 주소 + 카카오 URL" 이 되어 이미지가 깨진다.
     * 그래서 이미 http/https 로 시작하는 값은 그대로 통과시킨다.
     */
    private static String toProfileImgUrl(String profileImg) {
        if (profileImg == null || profileImg.isBlank()) {
            return null;
        }
        String lower = profileImg.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return profileImg; // 소셜 제공 외부 URL — 그대로 사용
        }
        return FileUrls.view(profileImg); // 업로드된 파일명 — 절대 URL 로 변환
    }
}

package com.lawchat.domain.user.dto.response;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;

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
        String profileImg,
        String socialProvider,
        UserStatus status,
        Boolean isAdmin,
        LocalDateTime createdAt
) {
    /** 엔티티 -> DTO 변환. password 는 의도적으로 포함하지 않는다. */
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImg(),
                user.getSocialProvider(),
                user.getStatus(),
                user.getIsAdmin(),
                user.getCreatedAt()
        );
    }
}

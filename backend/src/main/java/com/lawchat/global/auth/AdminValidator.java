package com.lawchat.global.auth;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 관리자 권한 검증 공통 컴포넌트.
 *
 * 인증 principal 이 단순 Long userId 라 Spring Security 의 Role 체계(@PreAuthorize)를 쓸 수 없어,
 * 서비스 레벨에서 users.is_admin 을 직접 확인한다.
 * 여러 서비스와 업로드 API 에서 같은 검증이 반복되므로 한 곳으로 모음.
 */
@Component
@RequiredArgsConstructor
public class AdminValidator {

    private final UserRepository userRepository;

    public void validate(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // is_admin 이 true 인지 null-safe 하게 확인 (Boolean 오토언박싱 NPE 방지)
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
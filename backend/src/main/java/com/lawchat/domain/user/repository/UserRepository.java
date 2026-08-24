package com.lawchat.domain.user.repository;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 조회/저장 담당.
 *
 * JpaRepository 를 상속하면 save, findById, delete 같은 기본 CRUD 는 자동 제공된다.
 * 아래 메서드들은 "쿼리 메서드" 방식으로, 메서드 이름을 Spring Data JPA 가 파싱해서
 * 실행 시점에 SQL 을 자동 생성해 준다. (구현 클래스를 직접 만들 필요 없음)
 *
 * 예) findByEmail            -> SELECT * FROM users WHERE email = ?
 *     existsByNickname       -> SELECT EXISTS(SELECT 1 FROM users WHERE nickname = ?)
 *     findBySocialProviderAndSocialId -> WHERE social_provider = ? AND social_id = ?
 *
 * 반환 타입을 Optional 로 두면 "없을 수도 있다"는 사실이 시그니처에 드러나서
 * 호출부에서 null 체크를 강제할 수 있다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인 시 이메일로 회원 조회 */
    Optional<User> findByEmail(String email);

    /** 회원가입 시 이메일 중복 확인 (탈퇴 회원 포함 — DB에 UNIQUE 제약이 걸려 있으므로) */
    boolean existsByEmail(String email);

    /** 닉네임 중복 확인 */
    boolean existsByNickname(String nickname);

    /** 소셜 로그인 시 provider + socialId 조합으로 기존 회원 조회 */
    Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);

    /** 특정 상태를 제외하고 조회할 때 사용 (예: 탈퇴 회원 제외) */
    Optional<User> findByUserIdAndStatusNot(Long userId, UserStatus status);
}

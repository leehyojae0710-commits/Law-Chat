package com.lawchat.domain.user.repository;

import com.lawchat.domain.user.entity.SocialProvider;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    /** 아이디 찾기/비밀번호 재설정 인증 시 전화번호로 회원 조회 */
    Optional<User> findByPhone(String phone);

    /** 회원가입 시 이메일 중복 확인 (탈퇴 회원 포함 — DB에 UNIQUE 제약이 걸려 있으므로) */
    boolean existsByEmail(String email);

    /** 회원가입 시 전화번호 중복 확인 */
    boolean existsByPhone(String phone);

    /** 닉네임 중복 확인 */
    boolean existsByNickname(String nickname);

    /** 소셜 로그인 시 provider + socialId 조합으로 기존 회원 조회 */
    Optional<User> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    /** 특정 상태를 제외하고 조회할 때 사용 (예: 탈퇴 회원 제외) */
    Optional<User> findByUserIdAndStatusNot(Long userId, UserStatus status);

    /**
     * 익명화 대상 조회 — 보존 기간이 지난 탈퇴 회원 중 아직 익명화되지 않은 회원.
     *
     * 조건이 세 개다.
     *  1) status = DELETED           : 탈퇴한 회원만
     *  2) deletedAt <= cutoff        : 보존 기간이 지난 회원만
     *                                  (cutoff = 오늘 - 보존일수. 이보다 과거에 탈퇴했으면 기간 만료)
     *  3) email 또는 socialId 가 있음 : 아직 익명화되지 않은 회원만
     *                                  이메일 가입자는 email 이, 소셜 가입자는 socialId 가 반드시 있으므로
     *                                  둘 다 null 이면 이미 처리가 끝난 것이다.
     *                                  이 조건이 없으면 배치가 돌 때마다 같은 회원을 계속 UPDATE 한다.
     *
     * 메서드 이름만으로는 이 조합을 표현할 수 없어 @Query 로 JPQL 을 직접 작성했다.
     */
    @Query("""
            SELECT u FROM User u
             WHERE u.status = :status
               AND u.deletedAt IS NOT NULL
               AND u.deletedAt <= :cutoff
               AND (u.email IS NOT NULL OR u.socialId IS NOT NULL)
            """)
    List<User> findAnonymizationTargets(@Param("status") UserStatus status,
                                        @Param("cutoff") LocalDateTime cutoff);
}
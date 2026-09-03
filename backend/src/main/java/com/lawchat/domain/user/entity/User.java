package com.lawchat.domain.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 회원 엔티티 — schema.sql 의 `users` 테이블과 1:1 매핑.
 *
 * [설계 원칙]
 * 1. setter 를 만들지 않는다.
 *    아무 곳에서나 user.setStatus(...) 를 호출할 수 있으면 상태 변경 규칙이 코드 전체에 흩어진다.
 *    대신 withdraw(), logout() 처럼 "의미가 담긴" 메서드만 열어둔다.
 * 2. 객체 생성도 new 대신 정적 팩토리(createLocalUser / createSocialUser)로만 한다.
 *    일반 가입자는 email+password 가 필수, 소셜 가입자는 social_id+provider 가 필수라
 *    생성자 하나로는 두 경우를 구분해서 강제할 수 없기 때문이다.
 * 3. JPA 는 프록시 생성을 위해 기본 생성자가 반드시 필요하므로
 *    protected 로만 열어두어 외부에서는 못 쓰게 막는다.
 */
@Entity
@Table(name = "users") // 테이블명은 "user" 가 아니라 "users" (schema.sql 기준)
public class User {

    /**
     * PK. DB가 AUTO_INCREMENT 이므로 IDENTITY 전략.
     * IDENTITY 는 INSERT 를 실제로 실행해야 채번된 값을 알 수 있으므로
     * persist() 시점에 즉시 INSERT 쿼리가 나간다는 특징이 있다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /** 일반 로그인 ID. 소셜 가입자는 null 일 수 있어 nullable. DB에 UNIQUE 제약 있음. */
    @Column(name = "email", length = 255)
    private String email;

    /** BCrypt 로 암호화된 비밀번호. 평문은 절대 저장하지 않는다. 소셜 가입자는 null. */
    @Column(name = "password", length = 255)
    private String password;

    /** 전화번호. 숫자만 정규화해서 저장한다(하이픈 없이, 예: 01012345678). 아이디 찾기/비밀번호 재설정 인증 수단. */
    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    /** 카카오/네이버 등이 내려주는 고유 회원 식별자 */
    @Column(name = "social_id", length = 255)
    private String socialId;

    /** 화면에 노출되는 이름. NOT NULL */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_img", length = 512)
    private String profileImg;

    /**
     * 소셜 로그인 제공자. DB 컬럼은 VARCHAR(50) 그대로이고,
     * EnumType.STRING 이라 "KAKAO"/"NAVER" 문자열로 저장/조회된다 (스키마 변경 없음).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", length = 50)
    private SocialProvider socialProvider;

    @Enumerated(EnumType.STRING) // ENUM 문자열 그대로 저장
    @Column(name = "status", nullable = false)
    private UserStatus status;

    /** 관리자 여부. MySQL tinyint(1) <-> Boolean 자동 매핑. */
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    /**
     * 현재 유효한 로그인 세션 식별값 (동시접속 차단용).
     *
     * 로그인할 때마다 새 UUID 로 덮어쓴다. 컬럼이 하나뿐이므로
     * "현재 유효한 로그인은 항상 딱 하나"가 구조적으로 보장된다.
     * 발급된 JWT 안에도 같은 값이 들어가고, 매 요청마다 이 값과 대조한다.
     *
     * null 이면 로그인 상태가 아니다(= 어떤 토큰도 통과 못 함).
     */
    @Column(name = "session_token", length = 64)
    private String sessionToken;

    /**
     * @CreationTimestamp : INSERT 시 Hibernate 가 현재 시각을 채워준다.
     * updatable = false 로 두어 이후 UPDATE 문에서 아예 제외시킨다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** @UpdateTimestamp : UPDATE 가 발생할 때마다 현재 시각으로 갱신된다. */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 탈퇴 시각. 미탈퇴 회원은 null (soft delete 방식) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ------------------------------------------------------------------
    // 생성자 / 정적 팩토리
    // ------------------------------------------------------------------

    /** JPA 전용 기본 생성자. 애플리케이션 코드에서 직접 호출 금지. */
    protected User() {
    }

    /**
     * 이메일 회원가입용.
     * @param encodedPassword 반드시 서비스에서 PasswordEncoder 로 암호화한 값을 넘길 것
     */
    public static User createLocalUser(String email, String encodedPassword, String nickname, String phone) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.phone = phone;
        user.status = UserStatus.ACTIVE;
        user.isAdmin = false;
        return user;
    }

    /**
     * 소셜 회원가입용.
     * 소셜은 우리 쪽에서 비밀번호를 관리하지 않으므로 password 는 null 로 둔다.
     *
     * @param email 소셜에서 받아온 이메일. 미동의/미제공이면 null 을 넘긴다.
     *              users.email 에 UNIQUE 제약이 있으므로, 이미 쓰이는 이메일이면
     *              서비스 레이어에서 null 로 바꿔 넘겨야 INSERT 가 실패하지 않는다.
     */
    public static User createSocialUser(String socialId, SocialProvider socialProvider,
                                        String nickname, String profileImg, String email) {
        User user = new User();
        user.socialId = socialId;
        user.socialProvider = socialProvider;
        user.nickname = nickname;
        user.profileImg = profileImg;
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.isAdmin = false;
        return user;
    }

    /**
     * 소셜 로그인 때마다 프로필 사진을 최신값으로 맞춰준다.
     *
     * 닉네임은 동기화하지 않는다. 사용자가 우리 서비스에서 직접 바꾼 닉네임을
     * 로그인할 때마다 카카오 값으로 되돌려 버리면 안 되기 때문이다.
     */
    public void syncSocialProfile(String profileImg) {
        if (profileImg != null && !profileImg.isBlank()) {
            this.profileImg = profileImg;
        }
    }

    // ------------------------------------------------------------------
    // 도메인 메서드 — 상태 변경은 전부 여기를 통해서만
    // ------------------------------------------------------------------

    /**
     * 프로필 수정. null 로 들어온 값은 "변경하지 않음"으로 처리한다(부분 수정).
     *
     * 영속 상태(=조회해온) 엔티티의 필드를 바꾸면 트랜잭션 커밋 시점에
     * Hibernate 가 변경 감지(dirty checking)로 UPDATE 를 자동 생성한다.
     * 따라서 repository.save() 를 다시 호출할 필요가 없다.
     */
    public void updateProfile(String nickname, String profileImg) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 로그인 성공 시 호출.
     *
     * ★ 동시접속 차단의 핵심 지점
     *   sessionToken 을 새 값으로 덮어쓴다. 이 컬럼은 하나뿐이므로
     *   이전 기기에 발급됐던 토큰 안의 값은 그 즉시 DB 값과 달라진다.
     *   → 이전 기기가 다음 요청을 보내는 순간 401 로 차단된다.
     *
     * @param newSessionToken 서비스에서 생성한 새 세션 식별값(UUID)
     */
    public void login(String newSessionToken) {
        this.status = UserStatus.ACTIVE;
        this.sessionToken = newSessionToken;
    }

    /**
     * 로그아웃.
     *
     * sessionToken 을 비우면 그 즉시 이 사용자에게 발급됐던 모든 JWT 가 무효가 된다.
     * (JWT 자체는 아직 만료 전이지만, DB 대조 단계에서 걸러진다)
     * 토큰 만료를 기다릴 필요 없이 즉시 차단되는 것이 이 방식의 장점이다.
     */
    public void logout() {
        this.status = UserStatus.LOGOUT;
        this.sessionToken = null;
    }

    /**
     * 요청으로 들어온 토큰의 세션값이 현재 유효한지 검사.
     *
     * false 가 되는 경우는 두 가지다.
     *   1) 로그아웃했다 (sessionToken 이 null)
     *   2) 다른 기기에서 새로 로그인했다 (sessionToken 이 다른 값으로 바뀜)
     */
    public boolean isSessionValid(String tokenSessionValue) {
        return this.sessionToken != null
                && this.sessionToken.equals(tokenSessionValue);
    }

    /**
     * 탈퇴(soft delete). 실제 row 는 지우지 않고 상태와 탈퇴 시각만 기록한다.
     *
     * ★ 이 시점에는 개인정보를 지우지 않는다.
     *   탈퇴 직후에는 분쟁 대응·문의 확인 등을 위해 원본 정보가 필요할 수 있으므로,
     *   보존 기간(application.yml 의 user.withdrawal.retention-days) 동안 그대로 둔다.
     *   기간이 지나면 UserAnonymizationScheduler 가 anonymize() 를 호출해 개인정보를 파기한다.
     *
     * ★ row 를 지우지 않는 이유
     *   chat_sessions / precedent_bookmarks 가 ON DELETE CASCADE 로 물려 있어,
     *   users row 를 실제로 삭제하면 상담 기록과 즐겨찾기가 통째로 함께 사라진다.
     *   상담 기록은 익명 상태로 보존해야 하므로 row 는 반드시 남긴다.
     */
    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.sessionToken = null; // 탈퇴 즉시 기존 토큰 무효화
    }

    /**
     * 개인정보 익명화(파기). 보존 기간이 지난 탈퇴 회원에게 배치가 호출한다.
     *
     * ★ 핵심 개념
     *   "row 를 지우는 것"이 아니라 "누구인지 알 수 없게 만드는 것"이다.
     *   상담 기록(chat_sessions, chat_messages)은 user_id 로 연결되어 그대로 남고,
     *   그 user_id 가 가리키는 회원에게서 개인 식별 정보만 사라진다.
     *   → 상담 데이터는 통계·품질 개선·분쟁 대응용으로 계속 쓸 수 있고,
     *     개인정보는 파기되어 개인정보보호법상 파기 의무를 충족한다.
     *
     * ★ 각 필드를 이렇게 처리하는 이유
     *   - email : null 로 비운다. MySQL 의 UNIQUE 제약은 NULL 을 중복으로 보지 않으므로
     *             여러 탈퇴 회원이 동시에 null 이어도 문제없고, 원래 이메일로 재가입도 가능해진다.
     *   - password : 로그인 수단이므로 즉시 제거. 해시라도 남길 이유가 없다.
     *   - socialId / socialProvider : 카카오 회원번호도 개인 식별자이므로 제거.
     *             제거하면 같은 카카오 계정으로 다시 로그인해도 신규 가입으로 처리된다.
     *   - profileImg : 얼굴 사진 등이 담길 수 있으므로 제거.
     *   - nickname : DB 에서 NOT NULL 이라 null 을 넣을 수 없다.
     *             그래서 개인을 특정할 수 없는 고정 형식으로 대체한다.
     *             userId 를 붙이는 것은 익명성을 해치지 않으면서(이미 PK 로 공개된 값)
     *             닉네임 중복 검사에 걸리지 않게 하기 위함이다.
     *
     * ★ status 와 deletedAt 은 유지한다.
     *   "탈퇴한 회원"이라는 사실과 시점 자체는 개인정보가 아니며,
     *   이 값이 있어야 배치가 이미 처리한 대상을 다시 건드리지 않는다.
     */
    public void anonymize() {
        this.email = null;
        this.password = null;
        this.phone = null;
        this.socialId = null;
        this.socialProvider = null;
        this.profileImg = null;
        this.sessionToken = null;
        this.nickname = "탈퇴회원_" + this.userId;
    }

    /** 탈퇴 회원 복구 */
    public void reactivate() {
        this.status = UserStatus.ACTIVE;
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.status == UserStatus.DELETED;
    }

    /** 소셜 가입자인지 여부 (= 비밀번호 로그인 불가) */
    public boolean isSocialUser() {
        return this.socialProvider != null;
    }

    // ------------------------------------------------------------------
    // Getter (setter 는 의도적으로 만들지 않음)
    // ------------------------------------------------------------------
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getSocialId() { return socialId; }
    public String getNickname() { return nickname; }
    public String getProfileImg() { return profileImg; }
    public SocialProvider getSocialProvider() { return socialProvider; }
    public UserStatus getStatus() { return status; }
    public Boolean getIsAdmin() { return isAdmin; }
    public String getSessionToken() { return sessionToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}


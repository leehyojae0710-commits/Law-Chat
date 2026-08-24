package com.lawchat.domain.user.service;

import com.lawchat.infra.oauth.client.KakaoOAuthClient;
import com.lawchat.infra.oauth.dto.KakaoTokenResponse;
import com.lawchat.infra.oauth.dto.KakaoUserInfo;
import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.global.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원/인증 관련 비즈니스 로직.
 *
 * [트랜잭션 전략]
 *  클래스 레벨에 @Transactional(readOnly = true) 를 걸어 기본을 "읽기 전용"으로 둔다.
 *   - 읽기 전용이면 Hibernate 가 스냅샷 비교(변경 감지)를 건너뛰어 성능이 좋아지고,
 *   - 조회 메서드에서 실수로 데이터가 수정되는 사고를 막아준다.
 *  데이터를 바꾸는 메서드에만 @Transactional 을 다시 붙여 쓰기 모드로 승격시킨다.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** users.social_provider 에 저장할 값. 네이버/구글 추가 시 enum 으로 승격을 권장. */
    private static final String KAKAO = "KAKAO";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuthClient kakaoOAuthClient;

    /**
     * 생성자 주입.
     * 필드에 @Autowired 를 쓰는 대신 생성자로 받으면
     *  - final 로 선언해 불변성을 보장할 수 있고,
     *  - 테스트에서 가짜 객체를 넣기 쉬우며,
     *  - 순환 참조를 컴파일/기동 시점에 발견할 수 있다.
     * 생성자가 하나뿐이면 @Autowired 를 생략해도 Spring 이 자동으로 주입한다.
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       KakaoOAuthClient kakaoOAuthClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoOAuthClient = kakaoOAuthClient;
    }

    // ==================================================================
    // 회원가입
    // ==================================================================

    /**
     * 이메일 회원가입.
     *
     * 처리 순서
     *  1) 이메일 중복 확인  → 중복이면 409
     *  2) 닉네임 중복 확인  → 중복이면 409
     *  3) 비밀번호를 BCrypt 로 암호화 (평문 저장 절대 금지)
     *  4) 엔티티 생성 후 저장
     *  5) 바로 로그인 상태가 되도록 토큰까지 발급해 반환
     *
     * 참고: 동시에 같은 이메일로 요청이 들어오면 1)의 검사만으로는 막지 못한다.
     *       최종 방어선은 DB의 UNIQUE 제약이며, 그 경우
     *       DataIntegrityViolationException 이 발생해 500 이 아닌 별도 처리가 필요하다.
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocalUser(request.email(), encodedPassword, request.nickname());
        User saved = userRepository.save(user);

        log.info("회원가입 완료 - userId={}", saved.getUserId());

        String token = jwtTokenProvider.createAccessToken(saved);
        return AuthResponse.of(token, jwtTokenProvider.getExpiresInSeconds(), saved);
    }

    // ==================================================================
    // 로그인
    // ==================================================================

    /**
     * 이메일 + 비밀번호 로그인.
     *
     * ★ 보안 포인트
     *  "이메일이 없음"과 "비밀번호가 틀림"을 구분해서 응답하면
     *  공격자가 어떤 이메일이 가입돼 있는지 알아낼 수 있다(계정 열거 공격).
     *  그래서 두 경우 모두 동일한 LOGIN_FAILED 메시지로 응답한다.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 탈퇴 회원은 로그인 불가
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }

        // 소셜 가입자는 password 가 null 이므로 비밀번호 로그인을 시도하면 안내
        if (user.isSocialUser() || user.getPassword() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_USER_CANNOT_LOGIN_LOCALLY);
        }

        // matches(평문, 저장된해시). 반대로 넣으면 항상 false 가 나오니 순서 주의.
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 이전에 LOGOUT 상태였다면 ACTIVE 로 복귀.
        // 영속 상태 엔티티라 변경 감지로 UPDATE 가 자동 실행된다(save 호출 불필요).
        user.login();

        log.info("로그인 성공 - userId={}", user.getUserId());

        String token = jwtTokenProvider.createAccessToken(user);
        return AuthResponse.of(token, jwtTokenProvider.getExpiresInSeconds(), user);
    }

    // ==================================================================
    // 카카오 소셜 로그인
    // ==================================================================

    /**
     * [방식 A] 인가코드 방식.
     *
     * 흐름
     *   1) 프론트가 넘긴 인가코드를 카카오에 보내 액세스 토큰으로 교환한다.
     *      → 이 단계에서 client_secret 이 사용된다. (서버에서만 수행 가능)
     *   2) 발급받은 액세스 토큰으로 사용자 정보를 조회한다.
     *   3) 카카오 회원번호로 우리 회원을 찾거나 새로 가입시킨다.
     *   4) 우리 서비스의 JWT 를 발급해 돌려준다.
     *      ★ 카카오 액세스 토큰은 프론트에 돌려주지 않는다. 이후 인증은 우리 JWT 로만 한다.
     */
    @Transactional
    public AuthResponse kakaoLoginWithCode(String authorizationCode, String redirectUri) {

        KakaoTokenResponse token = kakaoOAuthClient.requestToken(authorizationCode, redirectUri);
        KakaoUserInfo userInfo = kakaoOAuthClient.requestUserInfo(token.accessToken());

        return processKakaoUser(userInfo);
    }

    /**
     * [방식 B] 액세스 토큰 방식 (프론트가 JS SDK 로 이미 로그인한 경우).
     *
     * 인가코드 교환 단계가 없으므로 client_secret 은 쓰이지 않는다.
     * 대신 "받은 토큰이 우리 앱 것인지" 검증하는 단계가 필수다.
     */
    @Transactional
    public AuthResponse kakaoLoginWithAccessToken(String kakaoAccessToken) {

        // ★ 이 한 줄이 없으면 남의 앱 토큰으로 로그인이 뚫린다
        kakaoOAuthClient.verifyAccessToken(kakaoAccessToken);

        KakaoUserInfo userInfo = kakaoOAuthClient.requestUserInfo(kakaoAccessToken);
        return processKakaoUser(userInfo);
    }

    /**
     * 카카오에서 받아온 사용자 정보로 로그인 또는 가입 처리 (두 방식의 공통 로직).
     *
     * 조회 키는 이메일이 아니라 카카오 회원번호(social_id)다.
     * 이메일은 동의항목이라 없을 수도 있고 사용자가 변경할 수도 있어 식별자로 부적합하다.
     */
    private AuthResponse processKakaoUser(KakaoUserInfo userInfo) {

        String socialId = String.valueOf(userInfo.id());
        String kakaoEmail = userInfo.getVerifiedEmailOrNull();

        User user = userRepository.findBySocialProviderAndSocialId(KAKAO, socialId)
                .orElseGet(() -> registerKakaoUser(socialId, userInfo, kakaoEmail));

        if (user.isDeleted()) {
            // 탈퇴했던 회원이 다시 카카오로 로그인하면 계정을 복구한다.
            // 정책상 재가입을 막아야 한다면 여기서 예외를 던지도록 바꾸면 된다.
            user.reactivate();
        }

        user.syncSocialProfile(userInfo.getProfileImageOrNull());
        user.login();

        log.info("카카오 로그인 성공 - userId={}", user.getUserId());

        String jwt = jwtTokenProvider.createAccessToken(user);
        return AuthResponse.of(jwt, jwtTokenProvider.getExpiresInSeconds(), user);
    }

    /** 카카오 신규 회원 가입 처리 */
    private User registerKakaoUser(String socialId, KakaoUserInfo userInfo, String kakaoEmail) {

        // 같은 이메일로 이미 "이메일 가입"한 계정이 있으면 UNIQUE 제약에 걸린다.
        // 자동으로 계정을 합치면 카카오 이메일 도용 시 계정 탈취로 이어질 수 있으므로,
        // 안전하게 이메일 로그인을 안내하고 중단한다.
        if (kakaoEmail != null && userRepository.existsByEmail(kakaoEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED_LOCALLY);
        }

        String nickname = resolveNickname(userInfo.getNicknameOrNull());

        User newUser = User.createSocialUser(
                socialId,
                KAKAO,
                nickname,
                userInfo.getProfileImageOrNull(),
                kakaoEmail          // 미동의 시 null 로 저장된다 (컬럼이 nullable)
        );

        User saved = userRepository.save(newUser);
        log.info("카카오 신규 가입 - userId={}", saved.getUserId());
        return saved;
    }

    // ==================================================================
    // 로그아웃 / 조회 / 탈퇴
    // ==================================================================

    /**
     * 로그아웃.
     *
     * ★ 한계 짚고 가기
     *  JWT 는 서버가 상태를 들고 있지 않으므로, 여기서 status 를 LOGOUT 으로 바꿔도
     *  이미 발급된 토큰은 만료 전까지 계속 유효하다.
     *  완전한 무효화가 필요하면 (1) Redis 블랙리스트 또는 (2) refresh token 테이블이 필요하다.
     *  현재 schema.sql 에는 둘 다 없으므로, 실무 수준 보안이 필요하면 테이블 추가를 권장한다.
     */
    @Transactional
    public void logout(Long userId) {
        User user = findActiveUser(userId);
        user.logout();
        log.info("로그아웃 - userId={}", userId);
    }

    /** 내 정보 조회 */
    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(findActiveUser(userId));
    }

    /** 프로필 수정 */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, String profileImg) {
        User user = findActiveUser(userId);

        // 닉네임을 실제로 바꾸는 경우에만 중복 검사 (본인 기존 닉네임은 통과시켜야 함)
        if (nickname != null && !nickname.equals(user.getNickname())
                && userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateProfile(nickname, profileImg);
        return UserProfileResponse.from(user);
    }

    /** 비밀번호 변경 — 현재 비밀번호 확인 후 교체 */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findActiveUser(userId);

        if (user.isSocialUser() || user.getPassword() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_USER_CANNOT_LOGIN_LOCALLY);
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    /** 회원 탈퇴 (soft delete) */
    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);
        user.withdraw();
        log.info("회원 탈퇴 - userId={}", userId);
    }

    /** 이메일 사용 가능 여부 (true = 사용 가능) */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /** 닉네임 사용 가능 여부 (true = 사용 가능) */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // ==================================================================
    // 내부 헬퍼
    // ==================================================================

    /** 존재하고 탈퇴하지 않은 회원을 가져온다. 없으면 예외. */
    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        return user;
    }

    /** 닉네임이 중복이면 뒤에 숫자를 붙여 사용 가능한 값을 만든다 (소셜 가입 시 사용). */
    private String resolveNickname(String base) {
        String candidate = (base == null || base.isBlank()) ? "사용자" : base;
        if (candidate.length() > 45) {
            candidate = candidate.substring(0, 45); // 컬럼 길이(50) 여유 확보
        }
        String result = candidate;
        int suffix = 1;
        while (userRepository.existsByNickname(result)) {
            result = candidate + suffix++;
        }
        return result;
    }
}

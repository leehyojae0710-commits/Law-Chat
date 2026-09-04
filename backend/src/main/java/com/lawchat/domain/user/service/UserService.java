package com.lawchat.domain.user.service;

import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.dto.response.AuthVerifyResponse;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.entity.SocialProvider;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.global.file.FileStorageService;
import com.lawchat.global.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final ProfileImageValidator profileImageValidator;
    private final RestClient restClient;

    @Value("${oauth.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.kakao.redirect-uri:http://localhost:5173/kakao/OAuth}")
    private String defaultKakaoRedirectUri;

    @Value("${oauth.naver.client-id:}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret:}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri:http://localhost:5173/naver/OAuth}")
    private String defaultNaverRedirectUri;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       FileStorageService fileStorageService,
                       ProfileImageValidator profileImageValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileStorageService = fileStorageService;
        this.profileImageValidator = profileImageValidator;
        this.restClient = RestClient.create();
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String rawPhone = request.phone();
        String cleanPhone = (rawPhone != null && !rawPhone.isBlank())
                ? rawPhone.replaceAll("[^0-9]", "")
                : null;

        if (cleanPhone != null && userRepository.existsByPhone(cleanPhone)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        User user = User.createLocalUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                cleanPhone
        );

        User savedUser = userRepository.save(user);
        return issueAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }

        if (user.isSocialUser()) {
            throw new BusinessException(ErrorCode.SOCIAL_USER_CANNOT_LOGIN_LOCALLY);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 새 세션 발급 = 기존 기기의 세션은 이 순간 무효화된다(동시접속 차단)
        user.login(generateSessionToken());
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse kakaoLoginWithCode(String code, String redirectUri) {
        String finalRedirectUri = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : defaultKakaoRedirectUri;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", finalRedirectUri);
        params.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            params.add("client_secret", kakaoClientSecret);
        }

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패", e);
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        String accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        Map<String, Object> userResponse;
        try {
            userResponse = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        if (userResponse == null || !userResponse.containsKey("id")) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        String socialId = String.valueOf(userResponse.get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userResponse.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : "카카오사용자";
        String profileImg = profile != null ? (String) profile.get("profile_image_url") : null;

        return processSocialLogin(SocialProvider.KAKAO, socialId, email, nickname, profileImg);
    }

    public String issueNaverState() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public AuthResponse naverLoginWithCode(String code, String state, String redirectUri) {
        String finalRedirectUri = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : defaultNaverRedirectUri;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("code", code);
        params.add("state", state);
        params.add("redirect_uri", finalRedirectUri);

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri("https://nid.naver.com/oauth2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("네이버 토큰 발급 실패", e);
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }

        String accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }

        Map<String, Object> userResponse;
        try {
            userResponse = restClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("네이버 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = userResponse != null ? (Map<String, Object>) userResponse.get("response") : null;
        if (responseMap == null || !responseMap.containsKey("id")) {
            throw new BusinessException(ErrorCode.NAVER_AUTH_FAILED);
        }

        String socialId = (String) responseMap.get("id");
        String email = (String) responseMap.get("email");
        String nickname = (String) responseMap.get("nickname");
        String profileImg = (String) responseMap.get("profile_image");

        return processSocialLogin(SocialProvider.NAVER, socialId, email, nickname, profileImg);
    }

    @Transactional
    public void logout(Long userId) {
        User user = getUser(userId);
        user.logout();
        log.info("사용자 로그아웃 완료 - userId={}", userId);
    }

    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(getUser(userId));
    }

    /**
     * 토큰 유효성 확인용 최소 정보 조회 (GET /api/auth/verify).
     *
     * getMyProfile 과 조회 대상은 같지만 응답 DTO 가 다르다.
     * 이 API 는 앱이 켜질 때마다 호출되므로, 목적에 불필요한 개인정보
     * (email, profileImg, createdAt 등)까지 매번 내려보내지 않도록 분리했다.
     */
    public AuthVerifyResponse verifyToken(Long userId) {
        return AuthVerifyResponse.from(getUser(userId));
    }

    /**
     * 프로필 수정.
     *
     * ★ phone 파라미터가 추가된 이유
     *   기존에는 nickname/profileImg 만 받았는데, 전화번호도 마이페이지에서
     *   수정 가능해야 한다는 요구사항이 추가되어 phone 을 받도록 확장했다.
     *
     * ★ 프론트가 안 바뀌어도 되는 이유
     *   기존 프론트는 요청 바디에 phone 필드를 아예 넣지 않는다.
     *   JSON 에 없는 필드는 Jackson 이 자동으로 null 로 역직렬화하므로,
     *   phone 파라미터는 자연스럽게 null 로 들어오고 User.updateProfile() 이
     *   null 이면 건드리지 않으므로 기존 동작(닉네임/프로필사진만 수정)이 그대로 유지된다.
     *   전화번호 수정 UI 가 추가되면 그때 프론트가 phone 필드를 body 에 얹기만 하면 된다.
     *
     * ★ 정규화 + 중복확인을 회원가입(signup)과 동일한 규칙으로 맞춘 이유
     *   phone 컬럼에 UNIQUE 제약이 있어, 형식이 다른 같은 번호("010-1234-5678" vs
     *   "01012345678")가 서로 다른 값으로 취급되어 중복 체크를 빠져나가면 안 된다.
     *   그래서 회원가입 때와 동일하게 숫자만 남기고 비교한다.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, String profileImg, String phone) {
        User user = getUser(userId);

        if (nickname != null && !nickname.isBlank() && !nickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        String cleanPhone = (phone != null && !phone.isBlank())
                ? phone.replaceAll("[^0-9]", "")
                : null;

        if (cleanPhone != null && !cleanPhone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(cleanPhone)) {
                throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
            }
        }

        user.updateProfile(nickname, profileImg, cleanPhone);
        return UserProfileResponse.from(user);
    }

    /**
     * 프로필 이미지 업로드 + 반영을 한 번에 처리한다.
     *
     * ★ 왜 "업로드 API 따로 + PATCH 따로" 가 아니라 한 번에 처리하는가
     *   2단계로 나누면 프론트가 (1) 업로드해서 URL 받고 (2) 그 URL 로 다시 PATCH 를
     *   호출해야 한다. 그런데 (1)만 성공하고 (2)가 실패하면 공유폴더에는 파일이
     *   남았는데 DB 에는 반영이 안 된 고아 파일이 생긴다.
     *   프로필 사진은 "올리는 즉시 내 사진이 바뀐다"가 유일한 시나리오이므로
     *   한 요청으로 묶는 편이 프론트도 단순하고 정합성도 깨지지 않는다.
     *
     * ★ 저장 순서 주의
     *   파일 저장을 먼저 하고 DB 를 나중에 갱신한다.
     *   반대로 하면 DB 는 새 파일명을 가리키는데 파일 저장이 실패해
     *   깨진 이미지가 노출될 수 있다. 지금 순서라면 DB 갱신이 실패해도
     *   기존 사진이 그대로 유지되고, 저장된 파일만 쓰이지 않은 채 남는다.
     *
     * ★ 이전 이미지는 지우지 않는다
     *   같은 사진을 여러 곳(캐시된 페이지, 이미 내려간 응답)에서 참조 중일 수 있고,
     *   소셜 로그인 계정은 profile_img 가 카카오/네이버 서버의 외부 URL 이라
     *   우리 공유폴더에 있지도 않다. 잘못 지우면 남의 파일을 건드리게 된다.
     *   미사용 파일 정리는 별도 배치의 몫으로 남긴다.
     *
     * @return 이미지가 반영된 최신 프로필. 프론트는 이 응답으로 바로 화면을 갱신하면 된다.
     */
    @Transactional
    public UserProfileResponse updateProfileImage(Long userId, MultipartFile file) {
        profileImageValidator.validate(file);

        User user = getUser(userId);

        String storedFilename = fileStorageService.store(file);

        // DB 에는 파일명만 저장한다(서버 위치가 바뀌어도 마이그레이션 불필요).
        // 절대 URL 로의 변환은 UserProfileResponse 가 담당한다.
        user.updateProfile(null, storedFilename, null);

        log.info("프로필 이미지 변경 - userId={}, filename={}", userId, storedFilename);

        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);

        if (user.getPassword() == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        user.changePassword(passwordEncoder.encode(newPassword));

        // 비밀번호를 바꿨는데 기존 세션이 살아있으면 의미가 없다.
        // 로그아웃 처리해서 새 비밀번호로 다시 로그인하도록 강제한다.
        user.logout();
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getUser(userId);
        user.withdraw();
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private AuthResponse processSocialLogin(SocialProvider provider, String socialId,
                                           String email, String nickname, String profileImg) {
        User user = userRepository.findBySocialProviderAndSocialId(provider, socialId)
                .map(existing -> {
                    existing.syncSocialProfile(profileImg);
                    existing.login(generateSessionToken());
                    return existing;
                })
                .orElseGet(() -> {
                    String uniqueNickname = (nickname != null && !nickname.isBlank())
                            ? nickname
                            : "사용자_" + UUID.randomUUID().toString().substring(0, 8);
                    while (userRepository.existsByNickname(uniqueNickname)) {
                        uniqueNickname = "사용자_" + UUID.randomUUID().toString().substring(0, 8);
                    }
                    User newUser = User.createSocialUser(socialId, provider, uniqueNickname, profileImg, email);
                    return userRepository.save(newUser);
                });

        return issueAuthResponse(user);
    }

    /**
     * 세션 식별값 생성.
     *
     * UUID 를 쓰는 이유: 충돌 가능성이 사실상 0 이고, 예측이 불가능하며,
     * 별도 라이브러리 없이 JDK 표준으로 만들 수 있기 때문이다.
     * 이 값은 비밀값이 아니라 "몇 번째 로그인인지" 구분하는 표식이므로
     * 암호학적 강도보다 유일성이 중요하다.
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 토큰 발급 공통 경로.
     *
     * ★ 여기서 sessionToken 을 반드시 확보한다.
     *   회원가입/소셜 신규가입 경로는 login() 을 거치지 않고 바로 이 메서드로 오기 때문에,
     *   빠져 있으면 sessionToken 이 null 인 채로 JWT 가 발급되어 곧바로 401 이 난다.
     */
    private AuthResponse issueAuthResponse(User user) {
        if (user.getSessionToken() == null) {
            user.login(generateSessionToken());
        }
        String accessToken = jwtTokenProvider.createAccessToken(user);
        long expiresIn = jwtTokenProvider.getExpiresInSeconds();
        return AuthResponse.of(accessToken, expiresIn, user);
    }
}
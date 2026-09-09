package com.lawchat.domain.user.service;

import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.dto.response.AvailabilityResponse;
import com.lawchat.domain.user.dto.response.AuthVerifyResponse;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.entity.SocialProvider;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.domain.verification.repository.IdVerificationRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.global.file.FileStorageService;
import com.lawchat.global.file.ImageUploadValidator;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    /** 탈퇴일 표기 형식. 화면이 그대로 쓸 수 있게 완성된 문장으로 내려준다. */
    private static final java.time.format.DateTimeFormatter WITHDRAWN_DATE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    /** 인증을 마친 뒤 복구까지 허용하는 시간(분). */
    private static final long RESTORE_VERIFY_WINDOW_MINUTES = 10;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;
    private final ImageUploadValidator imageUploadValidator;
    private final RestClient restClient;
    /** [계정 복구] 아이디 찾기 인증을 마쳤는지 확인할 때 쓴다. */
    private final IdVerificationRepository idVerificationRepository;

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
                       ImageUploadValidator imageUploadValidator,
                       IdVerificationRepository idVerificationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.fileStorageService = fileStorageService;
        this.imageUploadValidator = imageUploadValidator;
        this.idVerificationRepository = idVerificationRepository;
        this.restClient = RestClient.create();
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String rawPhone = request.phone();
        String cleanPhone = (rawPhone != null && !rawPhone.isBlank())
                ? rawPhone.replaceAll("[^0-9]", "")
                : null;

        // [탈퇴 회원 안내 — B안]
        //   같은 이메일이나 전화번호로 다시 가입하려 하면 "이미 계정이 있다" 고 알린다.
        //   예전에는 DUPLICATE_EMAIL(이미 사용 중인 이메일) 로만 막아서,
        //   본인이 탈퇴했던 계정인데도 남이 쓰는 이메일처럼 읽혔다.
        //   상담 기록이 그대로 남아 있는데 그것을 되찾을 방법을 안내받지 못했다.
        //
        //   이메일뿐 아니라 전화번호도 보는 이유 —
        //   같은 사람이 이메일만 바꿔 다시 가입하려는 경우가 흔하다.
        //   그때도 "기존 계정을 복구하시겠어요?" 로 안내하는 편이 낫다.
        User withdrawn = userRepository.findByEmailAndStatus(request.email(), UserStatus.DELETED)
                .orElse(null);
        if (withdrawn == null && cleanPhone != null) {
            withdrawn = userRepository.findByPhoneAndStatus(cleanPhone, UserStatus.DELETED)
                    .orElse(null);
        }
        if (withdrawn != null) {
            // 소셜 가입자는 이 화면에서 복구할 수 없어 일반 중복으로 처리한다.
            //   비밀번호가 없어 본인 확인 수단이 없기 때문이다.
            if (withdrawn.isSocialUser()) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            throw new BusinessException(ErrorCode.WITHDRAWN_USER_RESTORABLE,
                    withdrawnMessage(withdrawn));
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
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

        // [탈퇴 회원 복구 — A안]
        //   비밀번호가 맞는지 **먼저** 확인한 뒤에 "복구할 수 있다" 고 알린다.
        //   순서를 바꾸면 남의 이메일을 넣어보는 것만으로 그 계정의 탈퇴 여부를 알 수 있다.
        //   (계정 존재 여부가 새는 것이라, 로그인 실패와 구분되지 않게 두어야 한다)
        if (user.isDeleted()) {
            if (user.isSocialUser()) {
                // 소셜 가입자는 비밀번호가 없어 여기서 본인 확인을 할 수 없다.
                // 소셜 로그인 경로(processSocialLogin)에서 복구를 처리한다.
                throw new BusinessException(ErrorCode.WITHDRAWN_USER);
            }
            boolean passwordMatches = user.getPassword() != null
                    && passwordEncoder.matches(request.password(), user.getPassword());
            if (!passwordMatches) {
                // 비밀번호가 틀리면 일반 로그인 실패와 똑같이 응답한다.
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
            // 본인이 맞다. 다만 자동으로 되살리지는 않는다 —
            // 실수로 로그인했다가 계정이 복구되면 사용자가 의도하지 않은 결과가 된다.
            // 화면이 확인을 받은 뒤 /auth/restore 를 부르게 한다.
            //
            // 탈퇴일을 문구에 담아 보낸다. 화면이 다시 조회하지 않아도
            // "탈퇴일은 …입니다. 복구하시겠어요?" 를 그대로 띄울 수 있다.
            throw new BusinessException(ErrorCode.WITHDRAWN_USER_RESTORABLE,
                    withdrawnMessage(user));
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
        imageUploadValidator.validate(file);

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

    /**
     * 최근에 인증을 마쳤는지 확인한다.
     *
     * 아이디 찾기 인증(/api/verifications/verify-code)이 남긴 흔적(used_at)을 본다.
     * 시간 제한이 없으면 몇 달 전 기록만으로도 복구가 통과한다.
     */
    private boolean verifiedRecently(String contactValue) {
        if (contactValue == null || contactValue.isBlank()) return false;
        return idVerificationRepository
                .findFirstByAuthTargetAndIsVerifiedTrueAndUsedAtAfterOrderByUsedAtDesc(
                        contactValue, LocalDateTime.now().minusMinutes(RESTORE_VERIFY_WINDOW_MINUTES))
                .isPresent();
    }

    /**
     * [계정 복구] 탈퇴한 계정을 다시 활성 상태로 되돌린다.
     *
     * ★ 복구 자체는 상태값만 바꾸면 된다
     *   탈퇴는 soft delete 라 users row 를 지우지 않는다.
     *   상담 기록(chat_sessions)·즐겨찾기(precedent_bookmarks)는 user_id 로 물려 있어
     *   그대로 남아 있다. status 를 ACTIVE 로 되돌리면 접근이 그대로 살아난다.
     *
     * ★ 본인 확인은 기존 아이디 찾기 인증을 재사용한다
     *   전화번호(또는 이메일)로 인증코드를 받고 확인한 뒤 이 API 를 부른다.
     *   복구 전용 인증을 따로 만들지 않은 이유 — 그 인증만으로 이미 이메일을 알아내고
     *   비밀번호 재설정까지 갈 수 있어, 복구만 더 엄격하게 해도 막아지는 것이 없다.
     *
     * ★ 비밀번호는 묻지 않는다
     *   이 경로를 타는 사람은 대개 기억하지 못한다. (기억하면 그냥 로그인했을 것이다)
     *   복구한 뒤 비밀번호를 모르면 기존 비밀번호 찾기로 재설정하면 된다.
     *
     * ★ 로그인까지 시키지는 않는다
     *   인증만으로 세션까지 주면 비밀번호를 모르는 사람이 그대로 들어오게 된다.
     *   복구 후에는 평소대로 로그인하게 한다.
     *
     * ★ 보존 기간이 지난 계정은 복구되지 않는다
     *   배치가 anonymize() 로 이메일·전화번호를 이미 null 로 지웠기 때문에
     *   아래 조회 자체가 비어 나온다.
     *
     * @param contactValue 인증을 마친 연락처 (이메일 또는 전화번호)
     */
    @Transactional
    public void restoreAccount(String contactValue) {
        String raw = (contactValue == null) ? "" : contactValue.trim();
        if (raw.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 전화번호는 숫자만 남겨 저장하므로 조회 전에 같은 형태로 맞춘다.
        boolean isEmail = raw.contains("@");
        String normalized = isEmail ? raw : raw.replaceAll("[^0-9]", "");

        // 인증 단계를 건너뛰고 이 API 를 직접 호출하는 것을 막는다.
        if (!verifiedRecently(normalized)) {
            throw new BusinessException(ErrorCode.RESTORE_NOT_VERIFIED);
        }

        User user = (isEmail
                ? userRepository.findByEmailAndStatus(normalized, UserStatus.DELETED)
                : userRepository.findByPhoneAndStatus(normalized, UserStatus.DELETED))
                .orElseThrow(() -> new BusinessException(ErrorCode.WITHDRAWN_USER_EXPIRED));

        user.reactivate();
        log.info("[계정 복구] userId={} 복구 완료", user.getUserId());
    }

    /**
     * [중복검사] 이메일을 쓸 수 있는지 확인한다.
     *
     * ★ 왜 탈퇴 계정을 따로 구분하는가
     *   users.email 에 UNIQUE 제약이 걸려 있어, 탈퇴 회원 row 가 남아 있는 한
     *   같은 이메일로는 새 가입이 **불가능**하다.
     *   예전에는 그 상황이 "이미 사용 중인 이메일" 로만 안내돼서,
     *   본인이 탈퇴했던 계정인데도 남이 쓰는 것처럼 읽혔고
     *   상담 기록이 그대로 남아 있는데 되찾을 방법을 안내받지 못했다.
     *
     * ★ 이메일은 이메일 인증으로 복구한다
     *   가입하려는 이메일 = 탈퇴 계정의 이메일 이므로, 그 주소로 코드를 보내
     *   받을 수 있는 사람인지 확인하면 본인 확인이 된다.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkEmail(String email) {
        String value = (email == null) ? "" : email.trim();
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        User found = userRepository.findByEmail(value).orElse(null);
        if (found == null) {
            return AvailabilityResponse.ok();
        }
        if (!found.isDeleted()) {
            return AvailabilityResponse.inUse();
        }

        // 소셜 가입자는 이 화면에서 복구할 수 없다. 소셜 로그인으로 안내해야 한다.
        if (found.isSocialUser()) {
            return AvailabilityResponse.withdrawnNotRestorable(found.getDeletedAt(), maskEmail(found.getEmail()),
                    "탈퇴한 소셜 계정이에요. 소셜 로그인으로 다시 이용해 주세요.");
        }

        // 가입하려는 이메일 = 탈퇴 계정의 이메일 이므로 그 주소로 인증한다.
        return AvailabilityResponse.withdrawnRestorable(
                found.getDeletedAt(), maskEmail(found.getEmail()), "EMAIL", value);
    }

    /**
     * [중복검사] 전화번호를 쓸 수 있는지 확인한다.
     *
     * 전화번호도 UNIQUE 제약이 걸려 있어 이메일과 사정이 같다.
     * 다만 복구 안내가 다르다 — 전화번호로 조회했으므로 **전화번호 인증**으로 복구한다.
     *
     * 어느 계정인지 알 수 있도록 가려진 이메일을 함께 준다.
     * (전화번호만 입력한 사용자는 그 계정의 이메일을 모를 수 있다)
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkPhone(String phone) {
        String digits = (phone == null) ? "" : phone.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        User found = userRepository.findByPhone(digits).orElse(null);
        if (found == null) {
            return AvailabilityResponse.ok();
        }
        if (!found.isDeleted()) {
            return AvailabilityResponse.inUse();
        }

        if (found.isSocialUser()) {
            return AvailabilityResponse.withdrawnNotRestorable(found.getDeletedAt(), maskEmail(found.getEmail()),
                    "탈퇴한 소셜 계정이에요. 소셜 로그인으로 다시 이용해 주세요.");
        }

        // 전화번호로 찾았으므로 그 번호로 인증한다. (이메일은 사용자가 모를 수 있다)
        return AvailabilityResponse.withdrawnRestorable(
                found.getDeletedAt(), maskEmail(found.getEmail()), "PHONE", digits);
    }

    /**
     * 탈퇴 계정 안내 문구를 만든다.
     *
     * 로그인·가입 어느 쪽에서 걸렸든 같은 문장을 쓴다.
     * 화면마다 조립하면 같은 상황에 다른 안내가 나간다.
     *
     * 남은 기간은 계산하지 않는다 — "며칠 뒤면 복구할 수 없다" 고 알리면
     * 사용자를 재촉하게 되고, 경계 근처에서 오차 하루로 안내와 실제가 어긋난다.
     */
    private String withdrawnMessage(User user) {
        String dateText = (user.getDeletedAt() == null) ? null
                : user.getDeletedAt().format(WITHDRAWN_DATE_FORMAT);
        String masked = maskEmail(user.getEmail());

        // 어느 계정인지 함께 알려준다. 화면이 문구를 조립하지 않아도 되게
        // 완성된 문장으로 내려준다. 화면마다 조립하면 같은 상황에 다른 안내가 나간다.
        return (masked != null ? masked + " 계정으로 가입한 이력이 있습니다. "
                               : "복구가 가능한 상태입니다. ")
                + (dateText != null ? "탈퇴일은 " + dateText + "입니다. " : "")
                + "복구하시겠습니까?";
    }

    /**
     * 이메일 일부를 가린다.
     *
     * 전화번호로 조회했을 때 "어느 계정인지" 는 알려주되 전체 주소는 노출하지 않는다.
     * 번호를 넣어보는 것만으로 그 사람의 이메일을 알아낼 수 있으면 안 된다.
     *   hong@example.com -> ho**@example.com
     */
    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "****";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "*" + domain;
        return local.substring(0, 2) + "*".repeat(local.length() - 2) + domain;
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
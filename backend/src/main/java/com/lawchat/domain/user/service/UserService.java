package com.lawchat.domain.user.service;

import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.entity.SocialProvider;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
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

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient restClient;

    @Value("${oauth.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.naver.client-id:}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret:}")
    private String naverClientSecret;

    public UserService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        // phone은 SignupRequest에서 @NotBlank가 아니라 선택 항목이라 null로 올 수 있다.
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

        user.login();
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse kakaoLoginWithCode(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri);
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
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("code", code);
        params.add("state", state);

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

    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, String profileImg) {
        User user = getUser(userId);

        if (nickname != null && !nickname.isBlank() && !nickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(nickname)) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        user.updateProfile(nickname, profileImg);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);

        if (user.getPassword() == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
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

    /**
     * 소셜 로그인 공통 처리.
     * User.createSocialUser 실제 시그니처: (socialId, socialProvider, nickname, profileImg, email)
     */
    private AuthResponse processSocialLogin(SocialProvider provider, String socialId,
                                             String email, String nickname, String profileImg) {
        User user = userRepository.findBySocialProviderAndSocialId(provider, socialId)
                .map(existing -> {
                    existing.syncSocialProfile(profileImg);
                    existing.login();
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
     * AuthResponse 실제 형태: (tokenType, accessToken, expiresIn, user) — refreshToken 필드 자체가 없다.
     * JwtTokenProvider 에도 createRefreshToken 메서드가 없다.
     */
    private AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        long expiresIn = jwtTokenProvider.getExpiresInSeconds();
        return AuthResponse.of(accessToken, expiresIn, user);
    }
}
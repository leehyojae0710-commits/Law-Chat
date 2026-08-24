package com.lawchat.domain.user.controller;

import com.lawchat.domain.user.dto.request.KakaoLoginRequest;
import com.lawchat.domain.user.dto.request.KakaoTokenLoginRequest;
import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증(로그인/회원가입/로그아웃) 전용 컨트롤러.
 *
 * 회원 정보 CRUD(UserController)와 분리해 두면
 * "로그인 없이 접근 가능한 URL"이 /api/auth 하위로 모여서
 * SecurityConfig 의 permitAll 설정이 단순해진다.
 *
 * @RestController = @Controller + @ResponseBody
 *   → 반환한 객체를 Jackson 이 자동으로 JSON 으로 바꿔 응답 바디에 실어준다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 회원가입
     * POST /api/auth/signup
     *
     * @Valid : SignupRequest 에 붙은 검증 애노테이션을 실행시킨다.
     *          실패하면 컨트롤러 진입 전에 예외가 발생하고 GlobalExceptionHandler 가 400 으로 변환한다.
     * @RequestBody : 요청 본문 JSON 을 DTO 객체로 역직렬화한다.
     *
     * 201 Created 를 반환하는 이유: 새 리소스(회원)가 생성되었기 때문.
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인
     * POST /api/auth/login
     *
     * 성공 시 accessToken 을 반환한다.
     * 프론트는 이후 모든 요청에 아래 헤더를 붙이면 된다.
     *   Authorization: Bearer {accessToken}
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * [방식 A] 카카오 로그인 — 인가코드 방식 (권장)
     * POST /api/auth/kakao
     *
     * 프론트 흐름
     *   1. 사용자를 아래 주소로 보낸다.
     *      https://kauth.kakao.com/oauth/authorize
     *        ?client_id={REST API 키}&redirect_uri={등록한 URI}&response_type=code
     *   2. 로그인/동의 후 redirect_uri 로 ?code=xxxx 가 붙어 돌아온다.
     *   3. 그 code 만 이 API 로 보낸다.
     *
     * 서버가 code 를 액세스 토큰으로 교환할 때 client_secret 을 사용한다.
     * 즉 시크릿은 서버 밖으로 절대 나가지 않는다. ★가장 안전한 방식★
     *
     * 신규 사용자면 자동 가입 후 로그인 처리되며, 응답은 일반 로그인과 동일한 AuthResponse 다.
     */
    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(
                userService.kakaoLoginWithCode(request.code(), request.redirectUri()));
    }

    /**
     * [방식 B] 카카오 로그인 — 액세스 토큰 방식
     * POST /api/auth/kakao/token
     *
     * 프론트가 Kakao SDK for JavaScript 로 직접 로그인해 액세스 토큰을 이미 가진 경우 사용한다.
     * 이 경로에서는 client_secret 이 쓰이지 않는 대신,
     * 서버가 access_token_info API 로 "우리 앱 토큰인지" 반드시 검증한다.
     */
    @PostMapping("/kakao/token")
    public ResponseEntity<AuthResponse> kakaoLoginWithToken(
            @Valid @RequestBody KakaoTokenLoginRequest request) {
        return ResponseEntity.ok(
                userService.kakaoLoginWithAccessToken(request.kakaoAccessToken()));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     *
     * @AuthenticationPrincipal : JwtAuthenticationFilter 가 SecurityContext 에 넣어둔
     * principal(여기서는 userId)을 파라미터로 바로 꺼내 쓴다.
     * 토큰에서 뽑은 값이므로 클라이언트가 위조할 수 없다.
     * (요청 바디로 userId 를 받으면 남의 계정을 조작할 수 있으니 절대 그렇게 하지 말 것)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ResponseEntity.noContent().build(); // 204
    }
}

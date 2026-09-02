package com.lawchat.domain.user.controller;

import com.lawchat.domain.user.dto.request.KakaoLoginRequest;
import com.lawchat.domain.user.dto.request.LoginRequest;
import com.lawchat.domain.user.dto.request.NaverLoginRequest;
import com.lawchat.domain.user.dto.request.SignupRequest;
import com.lawchat.domain.user.dto.response.AuthResponse;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 카카오 로그인 — 인가코드 방식
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
     * 네이버 로그인 시작 — state 발급
     * GET /api/auth/naver/state
     *
     * 프론트 흐름
     *   1. 이 API 를 먼저 호출해 state 를 받는다.
     *   2. 사용자를 아래 주소로 보낸다. 이때 1)에서 받은 state 를 그대로 실어야 한다.
     *      https://nid.naver.com/oauth2.0/authorize
     *        ?client_id={Client ID}&redirect_uri={등록한 URI}&response_type=code&state={받은 state}
     *   3. 로그인/동의 후 redirect_uri 로 ?code=xxxx&state=yyyy 가 붙어 돌아온다.
     *   4. code 와 그 state 를 그대로 POST /api/auth/naver 로 보낸다.
     *
     * state 가 필수인 이유는 네이버 스펙 요구사항이자 CSRF 방지용이다.
     * 상세: NaverStateProvider 주석 참고.
     */
    @GetMapping("/naver/state")
    public ResponseEntity<Map<String, String>> issueNaverState() {
        return ResponseEntity.ok(Map.of("state", userService.issueNaverState()));
    }

    /**
     * 네이버 로그인 — 인가코드 방식
     * POST /api/auth/naver
     *
     * code 와 함께 /naver/state 로 발급받았던 state 를 그대로 보내야 한다.
     * 서버가 위변조/만료 여부를 검증한 뒤에만 토큰 교환을 진행한다.
     *
     * 신규 사용자면 자동 가입 후 로그인 처리되며, 응답은 일반 로그인과 동일한 AuthResponse 다.
     */
    @PostMapping("/naver")
    public ResponseEntity<AuthResponse> naverLogin(@Valid @RequestBody NaverLoginRequest request) {
        return ResponseEntity.ok(
                userService.naverLoginWithCode(request.code(), request.state(), request.redirectUri()));
    }

    /**
     * 현재 로그인 상태 확인 (토큰 유효성 검증)
     * GET /api/auth/me
     *
     * ★ 이 메서드 본문에 도달했다는 사실 자체가 "토큰이 유효하다"는 증명이다.
     *   JwtAuthenticationFilter 가 이미 아래를 전부 마쳤기 때문이다.
     *     1. JWT 서명 검증 (위조 여부)
     *     2. 만료 시각 검증
     *     3. 토큰의 sessionToken 과 DB users.session_token 대조
     *     4. 탈퇴 회원 여부 확인
     *   하나라도 실패하면 여기 오지 못하고 401 이 응답된다.
     *   그래서 이 메서드 안에 별도 검증 로직이 없는 것이 정상이다.
     *
     * [프론트 사용 시나리오]
     *   앱 시작 시(새로고침/브라우저 재접속) 저장소에 토큰이 남아 있어도
     *   그것이 아직 살아있는지는 알 수 없다. 이 API 를 한 번 호출해서 판단한다.
     *     200 -> 유효. 응답으로 온 최신 회원 정보로 화면을 그린다.
     *     401 -> 무효. 저장소를 비우고 로그인 화면으로 보낸다.
     *            응답 body 의 code 로 사유를 구분할 수 있다.
     *              SESSION_INVALIDATED : 다른 기기에서 로그인되어 밀려남
     *              INVALID_TOKEN       : 토큰 없음/만료/위조
     *
     * [왜 최신 정보를 다시 내려주는가]
     *   로그인 시점 이후에 닉네임/프로필 이미지가 바뀌었거나
     *   관리자 권한이 부여/회수됐을 수 있다. 토큰 안의 정보는 발급 시점에 고정되므로
     *   DB 의 현재 값을 내려주어 프론트가 최신 상태로 갱신하게 한다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
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

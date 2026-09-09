package com.lawchat.domain.verification.controller;

import com.lawchat.domain.verification.dto.request.SendCodeRequest;
import com.lawchat.domain.verification.dto.request.VerifyCodeRequest;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 시 연락처 인증.
 *
 * ★ 아이디 찾기(/api/verification/id)와 경로를 나눈 이유
 *   같은 인증 저장소를 쓰지만 **발송 조건이 정반대**다.
 *     아이디 찾기 : 가입된 연락처에만 보낸다.
 *                  없는 번호에 보내면 넣어보는 것만으로 가입 여부를 알 수 있다.
 *     가입 인증   : 아직 가입하지 않은 번호가 본인 것인지 확인하는 절차라
 *                  DB 에 없어도 보내야 한다.
 *
 *   한 엔드포인트에 플래그를 받아 처리하면 화면이 그 값을 잘못 넘겼을 때
 *   아이디 찾기에서 가입 여부가 새어 나간다. 경로로 나누면 그 실수가 생기지 않는다.
 *
 * ★ 로그인 없이 호출된다
 *   가입 전이므로 인증 토큰이 없다. SecurityConfig 의 permitAll 대상에 추가해야 한다.
 */
@RestController
@RequestMapping("/api/verification/signup")
public class SignupVerificationController {

    private final VerificationService verificationService;

    public SignupVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * POST /api/verification/signup/send-code
     *
     * 가입 여부와 무관하게 인증코드를 보낸다.
     * 이미 사용 중인 연락처인지는 중복검사(/api/users/check-phone)에서 따로 확인한다.
     */
    @PostMapping("/send-code")
    public ResponseEntity<VerificationResultResponse> sendCode(
            @Valid @RequestBody SendCodeRequest request) {
        return ResponseEntity.ok(verificationService.sendCode(request, false));
    }

    /**
     * POST /api/verification/signup/verify-code
     *
     * 확인만 하고 아무것도 돌려주지 않는다.
     * 인증 흔적은 서버에 남고, 가입 요청 시 그 흔적이 최근 것인지 확인한다.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<VerificationResultResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        verificationService.verifySignupCode(request);
        return ResponseEntity.ok(VerificationResultResponse.ok("인증이 완료되었습니다."));
    }
}

package com.lawchat.domain.verification.controller;

import com.lawchat.domain.verification.dto.request.SendCodeRequest;
import com.lawchat.domain.verification.dto.request.VerifyCodeRequest;
import com.lawchat.domain.verification.dto.response.FindIdResultResponse;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아이디(이메일) 찾기 전용 컨트롤러 — 이메일/전화번호(SMS) 둘 다 지원.
 *
 * VerificationService, SendCodeRequest, VerifyCodeRequest, FindIdResultResponse,
 * IdVerification(Repository), SmsSender 는 이미 구현이 끝나 있었는데 이 컨트롤러가
 * 없어서 어떤 엔드포인트로도 호출할 수 없는 상태였다. 이번에 연결만 함.
 *
 * 로그인 전에 호출해야 하므로 SecurityConfig 의 "/api/verification/**" permitAll 규칙이 그대로 적용된다.
 */
@RestController
@RequestMapping("/api/verification/id")
public class IdVerificationController {

    private final VerificationService verificationService;

    public IdVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** POST /api/verification/id/send-code — EMAIL 또는 PHONE(SMS)으로 인증코드 발송 */
    @PostMapping("/send-code")
    public ResponseEntity<VerificationResultResponse> sendCode(
            @Valid @RequestBody SendCodeRequest request) {
        return ResponseEntity.ok(verificationService.sendCode(request));
    }

    /** POST /api/verification/id/verify-code — 인증코드 확인 성공 시 로그인 아이디(이메일) 반환 */
    @PostMapping("/verify-code")
    public ResponseEntity<FindIdResultResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(verificationService.verifyCode(request));
    }
}
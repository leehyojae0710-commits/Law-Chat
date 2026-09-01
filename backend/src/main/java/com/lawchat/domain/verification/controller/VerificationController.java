package com.lawchat.domain.verification.controller;

import com.lawchat.domain.verification.dto.request.PasswordResetRequest;
import com.lawchat.domain.verification.dto.request.PasswordResetSendCodeRequest;
import com.lawchat.domain.verification.dto.response.VerificationResultResponse;
import com.lawchat.domain.verification.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 전용 컨트롤러 (이메일 버전).
 *
 * 로그인 전에 호출해야 하므로 SecurityConfig 에 permitAll 로 등록돼 있어야 한다.
 * (/api/verification/** 로 이미 열어뒀다면 그대로 적용됨)
 *
 * 아이디 찾기 API는 전화번호 지원과 함께 나중에 별도로 추가할 예정이라 아직 포함하지 않았다.
 */
@RestController
@RequestMapping("/api/verification/password")
public class VerificationController {

    private final PasswordResetService passwordResetService;

    public VerificationController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /** POST /api/verification/password/send-code */
    @PostMapping("/send-code")
    public ResponseEntity<VerificationResultResponse> sendPasswordResetCode(
            @Valid @RequestBody PasswordResetSendCodeRequest request) {
        return ResponseEntity.ok(passwordResetService.sendCode(request));
    }

    /** POST /api/verification/password/reset — 코드 확인 + 비밀번호 변경을 한 번에 처리 */
    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.reset(request);
        return ResponseEntity.noContent().build();
    }
}

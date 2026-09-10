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
 * 탈퇴 계정 복구를 위한 연락처 인증.
 *
 * ★ 아이디 찾기와 절차가 같은데 왜 경로를 나눴나
 *   기능은 똑같다 — 가입된 연락처에 코드를 보내고 확인한다.
 *   다만 **메일 제목이 다르다.**
 *   복구하려는 사람이 "아이디 찾기 인증코드" 메일을 받으면
 *   잘못 온 메일로 오해하고 무시할 수 있다.
 *
 *   DB 에는 제목·본문을 저장하지 않으므로(코드와 연락처만 저장)
 *   경로만 나누면 되고 스키마 변경이 필요 없다.
 *
 * ★ 발송 조건은 아이디 찾기와 같다
 *   복구 대상은 이미 가입했던 사람이므로 requireExistingUser = true 다.
 *   없는 연락처에 보내면 넣어보는 것만으로 가입 여부를 알 수 있다.
 */
@RestController
@RequestMapping("/api/verification/restore")
public class RestoreVerificationController {

    private final VerificationService verificationService;

    public RestoreVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /** POST /api/verification/restore/send-code */
    @PostMapping("/send-code")
    public ResponseEntity<VerificationResultResponse> sendCode(
            @Valid @RequestBody SendCodeRequest request) {
        return ResponseEntity.ok(verificationService.sendCode(request, true, "계정 복구"));
    }

    /**
     * POST /api/verification/restore/verify-code
     *
     * 확인이 끝나면 어느 계정인지 알 수 있도록 이메일을 돌려준다.
     * (전화번호로 인증한 사용자는 그 번호에 어떤 계정이 묶여 있는지 모른다)
     * 아이디 찾기와 뒤처리가 같아 그 메서드를 그대로 쓴다.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<FindIdResultResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(verificationService.verifyCode(request));
    }
}

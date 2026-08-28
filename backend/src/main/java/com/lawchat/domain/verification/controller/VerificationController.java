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

@RestController
@RequestMapping("/api/verification/id")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/send-code")
    public ResponseEntity<VerificationResultResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        return ResponseEntity.ok(verificationService.sendCode(request));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<FindIdResultResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(verificationService.verifyCode(request));
    }
}
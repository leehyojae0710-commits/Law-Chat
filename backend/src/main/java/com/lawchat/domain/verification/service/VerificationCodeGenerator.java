package com.lawchat.domain.verification.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 6자리 숫자 인증코드 생성기.
 *
 * java.util.Random 대신 SecureRandom 을 쓰는 이유: Random 은 시드가 예측 가능해서
 * 인증/보안 목적의 난수 생성에는 부적합하다(암호학적으로 안전하지 않음).
 */
@Component
public class VerificationCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    /** "000000" ~ "999999" 사이의 6자리 문자열(0으로 시작할 수 있음, 항상 6자리 유지). */
    public String generate6Digit() {
        int number = random.nextInt(1_000_000); // 0 ~ 999999
        return String.format("%06d", number);
    }
}
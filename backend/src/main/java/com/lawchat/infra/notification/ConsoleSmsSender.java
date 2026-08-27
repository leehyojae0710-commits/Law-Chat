package com.lawchat.infra.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 실제 SMS 업체 연동 전까지 쓰는 임시 구현체.
 *
 * 문자를 실제로 보내지 않고 서버 콘솔/로그에만 찍는다. 개발 중 인증 코드를
 * 눈으로 확인하며 테스트할 수 있고, 나중에 CoolSMS 등 실제 업체 SDK 로
 * 교체할 때는 이 클래스를 지우고 SmsSender 를 구현하는 새 클래스로 바꾸기만 하면
 * VerificationService/PasswordResetService 쪽 코드는 전혀 수정할 필요가 없다.
 *
 * ★ 운영 배포 전 반드시 실제 SMS 발송 구현체로 교체할 것. 이대로 배포하면
 *   사용자는 문자를 받지 못하고 개발자만 서버 로그에서 코드를 볼 수 있다.
 */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String to, String message) {
        log.info("[SMS 발송 - 콘솔 대체 / 실제 문자 발송 아님] to={}, message={}", to, message);
    }
}
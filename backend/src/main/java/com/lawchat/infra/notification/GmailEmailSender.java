package com.lawchat.infra.notification;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gmail SMTP 를 통해 실제로 이메일을 발송하는 구현체.
 *
 * application.yml 의 spring.mail.* 설정(host=smtp.gmail.com, port=587 등)과
 * .env 의 GMAIL_USERNAME / GMAIL_APP_PASSWORD 를 사용한다.
 * JavaMailSender 빈은 spring-boot-starter-mail 의존성을 추가하면 자동으로 설정된다.
 *
 * ★ 구글 계정 비밀번호가 아니라 "앱 비밀번호"를 써야 한다.
 *   구글 계정 → 보안 → 2단계 인증 활성화 → 앱 비밀번호에서 16자리 값을 발급받아
 *   GMAIL_APP_PASSWORD 에 넣는다. 일반 로그인 비밀번호는 SMTP 인증에 쓸 수 없다.
 */
@Component
public class GmailEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public GmailEmailSender(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("이메일 발송 완료 - to={}", to);
        } catch (MailException e) {
            // 시크릿/앱 비밀번호가 메시지에 남지 않도록 예외 메시지만 로깅
            log.warn("이메일 발송 실패 - to={}, error={}", to, e.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}

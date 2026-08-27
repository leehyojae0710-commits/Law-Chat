package com.lawchat.global.config;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.infra.notification.EmailSender;
import com.lawchat.infra.notification.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class NotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    // JavaMailSender를 스프링 자동구성에 맡기지 않고 수동으로 확실히 생성
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    // EmailSender 빈을 직접 등록
    @Bean
    @Primary
    public EmailSender emailSender(JavaMailSender mailSender) {
        return (to, subject, body) -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailUsername);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);
                log.info("이메일 발송 완료 - to={}", to);
            } catch (MailException e) {
                log.warn("이메일 발송 실패 - to={}, error={}", to, e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            }
        };
    }

    // SmsSender 모의 빈
    @Bean
    public SmsSender smsSender() {
        return (to, body) -> {
            log.info("============== [SMS MOCK 발송] ==============");
            log.info("To  : {}", to);
            log.info("Body: {}", body);
            log.info("=============================================");
        };
    }
}
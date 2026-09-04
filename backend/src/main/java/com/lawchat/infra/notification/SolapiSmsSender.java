package com.lawchat.infra.notification;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * SOLAPI(https://solapi.com, 옛 CoolSMS)를 통해 실제로 SMS를 발송하는 구현체.
 *
 * application.yml 의 solapi.* 설정과 .env 의 SOLAPI_API_KEY / SOLAPI_API_SECRET /
 * SOLAPI_SENDER_NUMBER 를 사용한다. API 키는 SOLAPI 콘솔(console.solapi.com) >
 * 개발자정보 > API Key 관리에서 발급받는다.
 *
 * ConsoleSmsSender 를 대체하는 @Primary 구현체이므로, SmsSender 를 주입받는 쪽
 * (VerificationService, PasswordResetService 등) 코드는 전혀 수정할 필요가 없다.
 *
 * ★ SOLAPI_SENDER_NUMBER 는 반드시 SOLAPI 콘솔에서 본인/사업자 명의로 사전 등록해
 *   인증을 마친 발신번호여야 한다. 등록되지 않은 번호로는 발송 자체가 거부된다.
 */
@Component
@Primary
public class SolapiSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SolapiSmsSender.class);

    private final DefaultMessageService messageService;
    private final String senderNumber;

    public SolapiSmsSender(
            @Value("${solapi.api-key}") String apiKey,
            @Value("${solapi.api-secret}") String apiSecret,
            @Value("${solapi.sender-number}") String senderNumber
    ) {
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        this.senderNumber = senderNumber;
    }

    @Override
    public void send(String to, String message) {
        Message solapiMessage = new Message();
        solapiMessage.setFrom(senderNumber);
        solapiMessage.setTo(to);
        solapiMessage.setText(message);

        try {
            messageService.send(solapiMessage, null);
            log.info("SMS 발송 완료 - to={}", to);
        } catch (Exception e) {
            // API 키/시크릿이 메시지에 남지 않도록 예외 메시지만 로깅
            log.warn("SMS 발송 실패 - to={}, error={}", to, e.getMessage());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }
}
package com.lawchat.infra.notification;

/**
 * SMS(문자) 발송 인터페이스.
 *
 * 현재는 ConsoleSmsSender(콘솔 로그 출력)만 구현되어 있다.
 * 실제 문자 발송이 필요해지면 CoolSMS/알리고/NHN Cloud 등의 SDK를 사용하는
 * 새 구현체를 만들어 @Primary 로 지정하거나, ConsoleSmsSender 를 대체하면 된다.
 * (예: CoolSmsSender implements SmsSender)
 */
public interface SmsSender {

    /**
     * @param to      수신 전화번호 (예: 01012345678, 하이픈 없이)
     * @param message 문자 내용
     */
    void send(String to, String message);
}
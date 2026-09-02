package com.lawchat.infra.notification;

/**
 * 이메일 발송 인터페이스.
 *
 * VerificationService/PasswordResetService 는 이 인터페이스에만 의존하고,
 * 실제 구현(GmailEmailSender 등)은 모른다. 나중에 SendGrid/네이버클라우드 등으로
 * 발송 수단을 바꿔도 이 인터페이스를 구현하는 새 클래스만 추가하면 되고
 * 서비스 코드는 전혀 손댈 필요가 없다.
 */
public interface EmailSender {

    /**
     * @param to      수신 이메일 주소
     * @param subject 제목
     * @param body    본문(텍스트)
     */
    void send(String to, String subject, String body);
}

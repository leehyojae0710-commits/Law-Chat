package com.lawchat.domain.user.scheduler;

import com.lawchat.domain.user.service.UserAnonymizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 탈퇴 회원 개인정보 익명화 배치.
 *
 * ★ 스케줄러는 "언제 돌릴지"만 책임진다. 실제 로직은 UserAnonymizationService 에 있다.
 *
 * ★ @Scheduled 가 동작하려면 LawChatApplication 에 @EnableScheduling 이 있어야 한다.
 *   없으면 이 메서드는 조용히 실행되지 않는다(에러도 안 남).
 */
@Component
public class UserAnonymizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserAnonymizationScheduler.class);

    private final UserAnonymizationService userAnonymizationService;

    public UserAnonymizationScheduler(UserAnonymizationService userAnonymizationService) {
        this.userAnonymizationService = userAnonymizationService;
    }

    /**
     * 매일 새벽 4시에 실행.
     *
     * cron 표현식은 6자리다 : 초 분 시 일 월 요일
     *   "0 0 4 * * *"  →  매일 04:00:00
     *
     * ★ 새벽 시간대를 고르는 이유
     *   사용자 트래픽이 가장 적은 시간이라 DB 부하가 서비스에 영향을 덜 준다.
     *
     * ★ 하루 한 번이면 충분한 이유
     *   보존 기간이 6개월~1년 단위라, 파기가 몇 시간 늦어지는 것은 문제가 되지 않는다.
     *   오히려 자주 돌리면 의미 없는 조회 쿼리만 늘어난다.
     *
     * ★ 예외를 잡아주는 이유
     *   @Scheduled 메서드에서 예외가 밖으로 나가면 이후 스케줄이 중단될 수 있다.
     *   익명화 실패가 서비스 전체를 멈추게 해서는 안 되므로 로그만 남기고 넘어간다.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void anonymizeExpiredWithdrawnUsers() {
        try {
            int count = userAnonymizationService.anonymizeExpiredUsers();
            if (count > 0) {
                log.info("[배치] 탈퇴 회원 익명화 {}건 처리", count);
            }
        } catch (Exception e) {
            log.error("[배치] 탈퇴 회원 익명화 중 오류 발생", e);
        }
    }
}

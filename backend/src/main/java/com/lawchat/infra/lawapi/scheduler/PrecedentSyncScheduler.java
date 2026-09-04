package com.lawchat.infra.lawapi.scheduler;

import com.lawchat.domain.precedent.service.PrecedentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 서버 기동 시 판례를 초기 적재하는 러너.
 *
 * law-api.sync.enabled-on-startup=true 일 때, 검색어/날짜 조건 없이
 * PrecedentSyncService.syncAll()로 판례 전체를 수집한다.
 * (예전의 8개 키워드(민사/형사/손해배상/사기/임대차/이혼/교통사고/근로기준법) 방식은 제거했다 -
 *  커버리지가 좁아서 형사 세부/행정/지식재산권 판례가 거의 안 들어왔었다.)
 *
 * 전체 판례는 규모가 커서(수십만 건으로 추정) 완료까지 몇 시간~반나절 걸릴 수 있다.
 * ApplicationRunner를 동기로 블로킹하면 서버 기동 자체가 그동안 안 끝난 것처럼 걸리므로,
 * 별도 데몬 스레드에서 실행해 서버는 정상적으로 바로 뜨게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrecedentSyncScheduler implements ApplicationRunner {

    private final PrecedentSyncService precedentSyncService;

    @Value("${law-api.sync.enabled-on-startup:false}")
    private boolean enabledOnStartup;

    /**
     * 페이지당 100건이므로, 전체 규모(현재 추정 수십만 건)를 다 커버하려면 넉넉히 잡아야 한다.
     * application.yml에서 law-api.sync.startup-max-pages 로 조절 가능. 기본값 3000 -> 최대 30만 건.
     */
    @Value("${law-api.sync.startup-max-pages:3000}")
    private int startupMaxPages;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabledOnStartup) {
            log.info("law-api.sync.enabled-on-startup=false 라서 기동 시 판례 동기화를 건너뜁니다.");
            return;
        }

        log.info("판례 전체 수집을 백그라운드 스레드에서 시작합니다. maxPages={} (페이지당 100건)", startupMaxPages);

        Thread syncThread = new Thread(() -> {
            try {
                int processed = precedentSyncService.syncAll(startupMaxPages);
                log.info("기동 시 판례 전체 수집이 종료되었습니다. 처리 건수={}", processed);
            } catch (Exception e) {
                log.error("기동 시 판례 전체 수집 중 오류가 발생했습니다.", e);
            }
        }, "precedent-startup-sync");

        // 서버 종료 시 이 스레드가 프로세스 종료를 막지 않도록 데몬으로 실행한다.
        syncThread.setDaemon(true);
        syncThread.start();
    }
}
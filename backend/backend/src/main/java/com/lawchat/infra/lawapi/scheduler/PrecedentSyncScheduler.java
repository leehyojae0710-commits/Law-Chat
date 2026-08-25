package com.lawchat.infra.lawapi.scheduler;

import com.lawchat.domain.precedent.service.PrecedentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrecedentSyncScheduler implements ApplicationRunner {

    private final PrecedentSyncService precedentSyncService;

    @Value("${law-api.sync.enabled-on-startup:false}")
    private boolean enabledOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabledOnStartup) {
            log.info("law-api.sync.enabled-on-startup=false 라서 기동 시 판례 동기화를 건너뜁니다.");
            return;
        }

        log.info("판례 초기 데이터 적재를 시작합니다...");

        // 주요 키워드 정의
        List<String> keywords = List.of("민사", "형사", "손해배상", "사기", "임대차", "이혼", "교통사고", "근로기준법");

        for (String keyword : keywords) {
            try {
                log.info("키워드 [{}] 판례 수집 중 (최대 100건)...", keyword);
                
                // 파라미터: (query, date, maxCount)
                precedentSyncService.syncByQuery(keyword, null, 100);
                
                // 공공 API 부하 방지를 위해 0.5초 대기
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("키워드 [{}] 판례 수집 중 오류: {}", keyword, e.getMessage());
            }
        }

        log.info("판례 초기 데이터 수집이 완료되었습니다.");
    }
}
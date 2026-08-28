package com.lawchat.domain.precedent.controller;

import com.lawchat.domain.precedent.service.PrecedentSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 판례 동기화를 수동으로 실행하기 위한 관리자 API.
 *
 * TODO: global.security.SecurityConfig 구현 후 반드시 관리자(users.is_admin=1) 전용으로 제한할 것.
 *       현재는 SecurityConfig가 비어 있어(스캐폴딩 단계) 인증 없이 호출 가능한 상태이므로,
 *       배포 전 이 컨트롤러를 /api/admin/** 인가 규칙에 포함시켜야 한다.
 */
@RestController
@RequestMapping("/api/admin/precedents")
@RequiredArgsConstructor
public class PrecedentSyncController {

    private final PrecedentSyncService precedentSyncService;

    /**
     * 예)
     *   POST /api/admin/precedents/sync                         - 최근(어제 등록) 판례만 동기화
     *   POST /api/admin/precedents/sync?query=부당해고&maxPages=5  - 검색어 기준 동기화
     *   POST /api/admin/precedents/sync?date=2026-08-23           - 특정 등록일자 전체 동기화
     */
    @PostMapping("/sync")
    public SyncResult sync(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int maxPages
    ) {
        int processed = (query == null && date == null)
                ? precedentSyncService.syncRecent()
                : precedentSyncService.syncByQuery(query, date, maxPages);

        return new SyncResult(processed);
    }

    public record SyncResult(int processedCount) {
    }
}

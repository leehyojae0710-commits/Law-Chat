package com.lawchat.domain.precedent.service;

import com.lawchat.domain.precedent.entity.Precedent;
import com.lawchat.domain.precedent.repository.PrecedentRepository;
import com.lawchat.infra.lawapi.client.NationalLawApiClient;
import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.PrecedentDetail;
import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.PrecedentSummary;
import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * law.go.kr 판례 Open API -&gt; precedents 테이블 동기화.
 *
 * 호출 경로 (모두 이 클래스로 모인다 - 트리거만 다를 뿐 로직은 동일):
 *   1) 서버 기동 시 1회 : infra.lawapi.scheduler.PrecedentSyncScheduler (ApplicationRunner)
 *   2) 매일 정기 배치   : infra.lawapi.scheduler.PrecedentSyncScheduler (@Scheduled)
 *   3) 수동 실행(조건부) : domain.precedent.controller.PrecedentSyncController (POST /api/admin/precedents/sync)
 *   4) 수동 실행(전체)   : 위 컨트롤러에 all=true 파라미터 -&gt; syncAll()
 *
 * 판례는 사건번호(case_number) 기준으로 upsert한다 (테이블에 UNIQUE 제약이 걸려 있음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrecedentSyncService {

    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DISPLAY_PER_PAGE = 100; // law.go.kr 목록 조회 API의 페이지당 최대 건수
    private static final long DETAIL_FETCH_DELAY_MS = 150; // 상세조회 건당 호출 사이 지연 (공공 API 과호출 차단 방지)

    private final NationalLawApiClient nationalLawApiClient;
    private final PrecedentRepository precedentRepository;

    /**
     * 검색어/등록일자 조건으로 목록을 페이지 단위로 조회하면서 각 건을 본문 조회 후 저장한다.
     * 스케줄러와 수동 트리거 컨트롤러가 공통으로 사용하는 진입점이다.
     *
     * @param query          검색어. null/blank면 검색어 없이 조회(= date 조건만 적용, 또는 전체 최신순).
     * @param registeredDate 등록일자 필터. null이면 미적용.
     * @param maxPages       최대 조회 페이지 수 (API 과호출 방지용 안전장치).
     * @return 신규 저장 + 갱신된 건수
     */
    public int syncByQuery(String query, LocalDate registeredDate, int maxPages) {
        String date = registeredDate != null ? registeredDate.format(API_DATE_FORMAT) : null;
        int processed = 0;

        for (int page = 1; page <= maxPages; page++) {
            SearchResult result = nationalLawApiClient.search(query, date, page, DISPLAY_PER_PAGE);

            if (result.items().isEmpty()) {
                break;
            }

            for (PrecedentSummary summary : result.items()) {
                if (syncOne(summary.serialNumber())) {
                    processed++;
                }
                sleepBetweenDetailCalls();
            }

            boolean isLastPage = (long) page * DISPLAY_PER_PAGE >= result.totalCount();
            if (isLastPage) {
                break;
            }

            log.info("판례 동기화 진행 중. query={}, date={}, page={}, totalCnt={}, 누적 처리={}",
                    query, date, page, result.totalCount(), processed);
        }

        log.info("판례 동기화 완료. query={}, date={}, 처리 건수={}", query, date, processed);
        return processed;
    }

    /**
     * 스케줄러 기본 동작: "어제 등록된 판례"만 조회해 반영한다.
     * 매일 실행을 전제로 하므로 전체 재수집 없이 가볍게 증분 동기화가 된다.
     */
    public int syncRecent() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return syncByQuery(null, yesterday, 50);
    }

    /**
     * 검색어/등록일자 조건 없이 law.go.kr에 등록된 판례 전체를 최신순으로 페이지네이션하며 수집한다.
     * 법제처가 2013년 공개 당시 이미 "법원판례 8만3천여건"이라 밝혔고 그 뒤로 계속 쌓였으므로,
     * 지금 규모는 수십만 건일 수 있다 -&gt; 완료까지 몇 시간~반나절이 걸릴 수 있는 대량 작업이다.
     *
     * 사건번호 기준 upsert라 안전하게 재실행할 수 있다: 중간에 서버가 죽거나 멈춰도 다시 호출하면
     * 처음부터 페이지를 다시 넘기긴 하지만, 이미 저장된 건은 새로 만들지 않고 내용만 갱신한다.
     *
     * @param maxPages 최대 조회 페이지 수 (페이지당 {@value #DISPLAY_PER_PAGE}건). 전체를 다 받으려면
     *                 예상 총 건수 / {@value #DISPLAY_PER_PAGE} 이상으로 넉넉히 잡아야 한다.
     */
    public int syncAll(int maxPages) {
        log.info("판례 전체 수집을 시작합니다. maxPages={} (페이지당 {}건)", maxPages, DISPLAY_PER_PAGE);
        int processed = syncByQuery(null, null, maxPages);
        log.info("판례 전체 수집이 종료되었습니다. 처리 건수={}", processed);
        return processed;
    }

    /**
     * 상세조회(건당 API 1회 호출) 사이에 짧은 지연을 준다.
     * 특히 syncAll()처럼 수천 페이지를 순회할 때, 지연 없이 두드리면 공공 API가 요청을 차단할 수 있다.
     */
    private void sleepBetweenDetailCalls() {
        try {
            Thread.sleep(DETAIL_FETCH_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 판례 1건을 본문 조회해서 저장/갱신한다.
     * 이미 존재하는 사건번호면 내용만 최신화(synced_at 갱신)하고, 없으면 새로 만든다.
     * 개별 건 실패가 전체 동기화를 막지 않도록 예외를 여기서 흡수한다.
     */
    @Transactional
    public boolean syncOne(String serialNumber) {
        try {
            PrecedentDetail detail = nationalLawApiClient.fetchDetail(serialNumber);

            if (detail.caseNumber() == null || detail.fullText() == null) {
                log.warn("판례 본문이 비어 있어 건너뜁니다. serialNumber={}", serialNumber);
                return false;
            }

            Precedent fresh = toEntity(detail);
            precedentRepository.findByCaseNumber(detail.caseNumber())
                    .ifPresentOrElse(
                            existing -> existing.updateFrom(fresh),
                            () -> precedentRepository.save(fresh)
                    );
            return true;
        } catch (Exception e) {
            log.error("판례 저장 실패. serialNumber={}", serialNumber, e);
            return false;
        }
    }

    private Precedent toEntity(PrecedentDetail detail) {
        return Precedent.create(
                detail.caseNumber(),
                detail.caseName(),
                detail.courtName(),
                detail.courtTypeCode(),
                parseJudgmentDate(detail.judgmentDate()),
                detail.caseTypeName(),
                detail.holdingIssues(),
                detail.summary(),
                detail.referencedArticles(),
                detail.referencedCases(),
                detail.fullText()
        );
    }

    /**
     * law.go.kr API의 judgmentDate(선고일자)는 "yyyyMMdd" 형식 문자열로 온다.
     * 값이 없거나(null/blank) 형식이 어긋나면 저장을 막지 않고 null로 처리한다
     * (선고일자 필터를 안 쓰는 케이스는 여전히 정상 동작해야 하므로).
     */
    private LocalDate parseJudgmentDate(String judgmentDate) {
        if (judgmentDate == null || judgmentDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(judgmentDate.trim(), API_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("선고일자 파싱 실패, null로 저장합니다. judgmentDate={}", judgmentDate);
            return null;
        }
    }
}
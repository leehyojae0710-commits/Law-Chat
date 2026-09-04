package com.lawchat.domain.precedent.controller;

import com.lawchat.domain.precedent.dto.response.PrecedentAiSummaryResponse;
import com.lawchat.domain.precedent.dto.response.PrecedentBookmarkResponse;
import com.lawchat.domain.precedent.dto.response.PrecedentDetailResponse;
import com.lawchat.domain.precedent.dto.response.PrecedentListResponse;
import com.lawchat.domain.precedent.service.PrecedentSearchService;
import com.lawchat.domain.precedent.service.PrecedentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * /api/precedents/*
 *
 * 검색 필터(2026-09, 다중선택 확장):
 *  - category (복수)   : 사건종류 다중선택 - "전체" 또는 미지정 시 필터 없음.
 *                         DB에 실제로 존재하는 값 그대로 사용한다: 민사/형사/일반행정/가사/세무/특허/선거,특별
 *  - courtType (복수)  : "대법원"/"고등법원"/"하급심" 중 0개 이상 (court_name 패턴 기반 3단계 분류)
 *  - courtName         : 법원명 정확일치 (드롭다운, /api/precedents/court-names 목록에서 하나 선택)
 *  - caseNumber        : 사건번호 정확일치
 *  - caseName          : 사건명 부분일치 (LIKE)
 *  - referencedArticles : 참조조문 부분일치 (LIKE)
 *  - decidedDateFrom/To : 선고일자 범위 (yyyy-MM-dd)
 *
 * 쿼리 파라미터 형식: 같은 이름을 반복해서 보낸다 (예: ?category=민사&category=형사).
 * "category[]=..." 형식(대괄호)은 Spring이 List<String>으로 바인딩하지 못하므로 사용하지 않는다
 * (프론트 axios paramsSerializer가 이 형식으로 나가도록 설정돼 있음 - api/client.ts 참고).
 */
@RestController
@RequestMapping("/api/precedents")
@RequiredArgsConstructor
public class PrecedentController {

    private final PrecedentSearchService precedentSearchService;
    private final PrecedentService precedentService;

    @GetMapping
    public ResponseEntity<PrecedentListResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) Boolean aiSimilarity,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String caseNumber,
            @RequestParam(required = false) String caseName,
            @RequestParam(required = false) String referencedArticles,
            @RequestParam(required = false) List<String> courtType,
            @RequestParam(required = false) String courtName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decidedDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decidedDateTo
    ) {
        return ResponseEntity.ok(precedentSearchService.search(
                query, category, aiSimilarity, page, size,
                caseNumber, caseName, referencedArticles, courtType, courtName, decidedDateFrom, decidedDateTo
        ));
    }

    // 리터럴 경로가 "/{precedentId}"보다 우선 매칭되므로 순서와 무관하게 안전하다.
    @GetMapping("/court-names")
    public ResponseEntity<List<String>> getCourtNames() {
        return ResponseEntity.ok(precedentSearchService.getCourtNames());
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<PrecedentBookmarkResponse>> getBookmarks(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(precedentService.getBookmarks(userId));
    }

    @GetMapping("/{precedentId}")
    public ResponseEntity<PrecedentDetailResponse> getDetail(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long precedentId) {
        return ResponseEntity.ok(precedentService.getDetail(userId, precedentId));
    }

    @GetMapping("/{precedentId}/ai-summary")
    public ResponseEntity<PrecedentAiSummaryResponse> getAiSummary(@PathVariable Long precedentId) {
        return ResponseEntity.ok(precedentService.getAiSummary(precedentId));
    }

    @PostMapping("/{precedentId}/bookmark")
    public ResponseEntity<Void> addBookmark(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long precedentId) {
        precedentService.addBookmark(userId, precedentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{precedentId}/bookmark")
    public ResponseEntity<Void> removeBookmark(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long precedentId) {
        precedentService.removeBookmark(userId, precedentId);
        return ResponseEntity.noContent().build();
    }
}
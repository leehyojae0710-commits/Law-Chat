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
 * 프론트 매칭:
 *  - SearchBar, CategoryFilter, AiSimilaritySwitch -> GET /precedents
 *  - PrecedentResultCard 클릭(상세)                -> GET /precedents/{precedentId}
 *  - PrecedentResultCard "AI 요약 보기"             -> GET /precedents/{precedentId}/ai-summary
 *  - SavedPrecedentPanel                           -> GET /precedents/bookmarks,
 *                                                       POST/DELETE /precedents/{precedentId}/bookmark
 *
 * 목록/상세(GET)는 SecurityConfig에서 비로그인 열람을 허용한다.
 * 북마크 관련 3개 엔드포인트는 로그인이 필요하므로 SecurityConfig에 별도 authenticated 규칙을 추가해두었다.
 *
 * 검색 필터 확장(2차):
 *  - caseNumber        : 사건번호 정확일치
 *  - courtType         : "대법원" / "고등법원" / "하급심" 중 하나 (court_type_code가 대법원/그외 2종류뿐이라
 *                         court_name 문자열 패턴으로 3단계를 분류한다 - PrecedentRepository 참고)
 *  - courtName         : 법원명 정확일치 (드롭다운에서 특정 법원 하나를 선택하는 용도, courtType과 별개)
 *  - decidedDateFrom/To : 선고일자 범위 (yyyy-MM-dd), 둘 중 하나만 줘도 동작
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
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean aiSimilarity,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String caseNumber,
            @RequestParam(required = false) String courtType,
            @RequestParam(required = false) String courtName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decidedDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate decidedDateTo
    ) {
        return ResponseEntity.ok(precedentSearchService.search(
                query, category, aiSimilarity, page, size,
                caseNumber, courtType, courtName, decidedDateFrom, decidedDateTo
        ));
    }

    // 리터럴 경로("/bookmarks")가 "/{precedentId}"보다 우선 매칭되므로 순서와 무관하게 안전하다.
    @GetMapping("/bookmarks")
    public ResponseEntity<List<PrecedentBookmarkResponse>> getBookmarks(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(precedentService.getBookmarks(userId));
    }

    @GetMapping("/{precedentId}")
    public ResponseEntity<PrecedentDetailResponse> getDetail(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long precedentId) {
        return ResponseEntity.ok(precedentService.getDetail(userId, precedentId));
    }

    /**
     * AI(KoBART) 판례요약. legal_chatbot_ai POST /summarize/precedent 를 실시간 호출한다(저장 안 함).
     * 목록/상세와 마찬가지로 비로그인도 열람 가능하다.
     */
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

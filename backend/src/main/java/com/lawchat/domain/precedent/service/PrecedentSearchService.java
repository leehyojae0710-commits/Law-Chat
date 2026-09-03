package com.lawchat.domain.precedent.service;

import com.lawchat.domain.precedent.dto.projection.PrecedentSummaryView;
import com.lawchat.domain.precedent.dto.response.PrecedentListResponse;
import com.lawchat.domain.precedent.repository.PrecedentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * 판례 검색/목록 조회 (PrecedentSearchPage: SearchBar + CategoryFilter + AiSimilaritySwitch
 * + 사건번호/법원종류/법원명/선고일자 범위 필터).
 *
 * aiSimilarity는 현재 유사어 확장 검색 로직이 없어 일반 LIKE 검색으로 대체한다.
 * 추후 legal_chatbot_ai 쪽에 유사어 확장을 요청해서 query를 여러 개로 늘려 OR 검색하는 식으로
 * 확장할 수 있는 지점이라 파라미터는 그대로 받아만 두었다.
 *
 * courtType은 "대법원" / "고등법원" / "하급심" 중 하나만 허용한다. 그 외 값이 들어오면
 * 필터를 적용하지 않은 것과 동일하게 무시한다(잘못된 값으로 결과가 0건이 되는 걸 방지).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecedentSearchService {

    private static final String ALL_CATEGORY = "전체";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> VALID_COURT_TYPES = Set.of("대법원", "고등법원", "하급심");

    private final PrecedentRepository precedentRepository;

    public PrecedentListResponse search(String query, String category, Boolean aiSimilarity, Integer page, Integer size) {
        return search(query, category, aiSimilarity, page, size, null, null, null, null, null);
    }

    public PrecedentListResponse search(String query, String category, Boolean aiSimilarity, Integer page, Integer size,
                                         String caseNumber, String courtType, String courtName,
                                         LocalDate decidedDateFrom, LocalDate decidedDateTo) {
        String keyword = normalize(query);
        String caseTypeName = normalizeCategory(category);
        String normalizedCaseNumber = normalize(caseNumber);
        String normalizedCourtType = normalizeCourtType(courtType);
        String normalizedCourtName = normalize(courtName);

        Pageable pageable = PageRequest.of(
                resolvePage(page),
                resolveSize(size),
                Sort.by(Sort.Direction.DESC, "syncedAt")
        );

        // LOB 컬럼(holding/fullText/referencedArticles/referencedCases)을 빼고 목록에 필요한
        // 컬럼만 가져오는 프로젝션 쿼리 사용 — 검색이든 단순 페이지 이동이든 판례 전문을
        // 매번 통째로 읽어오지 않도록 한다.
        Page<PrecedentSummaryView> result = precedentRepository.searchSummary(
                keyword, caseTypeName, normalizedCaseNumber, normalizedCourtType, normalizedCourtName,
                decidedDateFrom, decidedDateTo, pageable
        );
        return PrecedentListResponse.fromSummaryView(result);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank() || ALL_CATEGORY.equals(category.trim())) {
            return null;
        }
        return category.trim();
    }

    private String normalizeCourtType(String courtType) {
        if (courtType == null || courtType.isBlank()) {
            return null;
        }
        String trimmed = courtType.trim();
        return VALID_COURT_TYPES.contains(trimmed) ? trimmed : null;
    }

    private int resolvePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}

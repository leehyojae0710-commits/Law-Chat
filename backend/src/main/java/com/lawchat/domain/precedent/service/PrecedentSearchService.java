package com.lawchat.domain.precedent.service;

import com.lawchat.domain.precedent.dto.projection.PrecedentSummaryView;
import com.lawchat.domain.precedent.dto.response.PrecedentListResponse;
import com.lawchat.domain.precedent.repository.PrecedentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecedentSearchService {

    private static final String ALL_CATEGORY = "전체";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> VALID_COURT_TYPES = Set.of("대법원", "고등법원", "하급심");

    private static final List<String> EMPTY_LIST_PLACEHOLDER = List.of("\u0000__NONE__");

    private static final String BOOLEAN_MODE_OPERATORS = "[+\\-*\"()~<>@]";

    private final PrecedentRepository precedentRepository;

    public PrecedentListResponse search(String query, List<String> categories, Boolean aiSimilarity, Integer page, Integer size,
                                         String caseNumber, String caseName, String referencedArticles,
                                         List<String> courtTypes, String courtName,
                                         LocalDate decidedDateFrom, LocalDate decidedDateTo) {
        String keyword = normalizeKeyword(query);
        Set<String> normalizedCategories = normalizeCategories(categories);
        String normalizedCaseNumber = normalize(caseNumber);
        String normalizedCaseName = normalize(caseName);
        String normalizedReferencedArticles = normalize(referencedArticles);
        Set<String> normalizedCourtTypes = normalizeCourtTypes(courtTypes);
        String normalizedCourtName = normalize(courtName);

        boolean hasAnyFilter = keyword != null || !normalizedCategories.isEmpty() || normalizedCaseNumber != null
                || normalizedCaseName != null || normalizedReferencedArticles != null
                || !normalizedCourtTypes.isEmpty() || normalizedCourtName != null
                || decidedDateFrom != null || decidedDateTo != null;

        // 사건종류만 (1개 이상) 선택하고 다른 필터/키워드는 없는 경우 - 가장 빠른 인덱스 전용 경로.
        boolean isCategoryOnlyFilter = !normalizedCategories.isEmpty() && keyword == null && normalizedCaseNumber == null
                && normalizedCaseName == null && normalizedReferencedArticles == null
                && normalizedCourtTypes.isEmpty() && normalizedCourtName == null
                && decidedDateFrom == null && decidedDateTo == null;

        boolean categoriesEmpty = normalizedCategories.isEmpty();
        boolean courtTypeEmpty = normalizedCourtTypes.isEmpty();
        List<String> categoriesParam = categoriesEmpty ? EMPTY_LIST_PLACEHOLDER : List.copyOf(normalizedCategories);
        boolean hasSupreme = normalizedCourtTypes.contains("대법원");
        boolean hasHigh = normalizedCourtTypes.contains("고등법원");
        boolean hasLower = normalizedCourtTypes.contains("하급심");

        long start = System.currentTimeMillis();
        Page<PrecedentSummaryView> result;
        if (isCategoryOnlyFilter) {
            Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
            result = precedentRepository.findByCaseTypeNameInOrderBySyncedAtDesc(normalizedCategories, pageable);
        } else if (keyword != null) {
            // 검색어가 실제로 있을 때만 FULLTEXT MATCH()가 낀 무거운 쿼리를 탄다.
            Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
            result = precedentRepository.searchSummary(
                    keyword, categoriesEmpty, categoriesParam, normalizedCaseNumber,
                    normalizedCaseName, normalizedReferencedArticles,
                    courtTypeEmpty, hasSupreme, hasHigh, hasLower,
                    normalizedCourtName, decidedDateFrom, decidedDateTo, pageable
            );
        } else if (hasAnyFilter) {
            // 키워드 없이 필터(법원종류/사건종류/법원명/기간)만 조합 - MATCH() 없는 경량 쿼리.
            // 법원종류 체크박스가 다중선택으로 바뀌면서 가장 자주 타게 된 경로라 여기서 분리한 게 핵심.
            Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size));
            result = precedentRepository.searchByFilters(
                    categoriesEmpty, categoriesParam, normalizedCaseNumber,
                    normalizedCaseName, normalizedReferencedArticles,
                    courtTypeEmpty, hasSupreme, hasHigh, hasLower,
                    normalizedCourtName, decidedDateFrom, decidedDateTo, pageable
            );
        } else {
            Pageable pageable = PageRequest.of(resolvePage(page), resolveSize(size),
                    Sort.by(Sort.Direction.DESC, "syncedAt"));
            result = precedentRepository.findAllBy(pageable);
        }
        long elapsedMs = System.currentTimeMillis() - start;
        log.info("[판례검색] keyword='{}' hasFilter={} categories={} page={} size={} -> {}ms (totalElements={})",
                keyword, hasAnyFilter, normalizedCategories, resolvePage(page), resolveSize(size), elapsedMs, result.getTotalElements());
        if (elapsedMs > 1000) {
            log.warn("[판례검색] 1초 이상 걸림 - keyword='{}' hasFilter={} elapsedMs={}", keyword, hasAnyFilter, elapsedMs);
        }

        return PrecedentListResponse.fromSummaryView(result);
    }

    public List<String> getCourtNames() {
        return precedentRepository.findDistinctCourtNames();
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String normalizeKeyword(String value) {
        String trimmed = normalize(value);
        if (trimmed == null) {
            return null;
        }
        String sanitized = trimmed.replaceAll(BOOLEAN_MODE_OPERATORS, " ").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private Set<String> normalizeCategories(List<String> categories) {
        if (categories == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String c : categories) {
            if (c == null || c.isBlank() || ALL_CATEGORY.equals(c.trim())) {
                continue;
            }
            result.add(c.trim());
        }
        return result;
    }

    private Set<String> normalizeCourtTypes(List<String> courtTypes) {
        if (courtTypes == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String c : courtTypes) {
            if (c == null) {
                continue;
            }
            String trimmed = c.trim();
            if (VALID_COURT_TYPES.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
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
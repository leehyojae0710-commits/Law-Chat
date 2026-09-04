package com.lawchat.domain.precedent.repository;

import com.lawchat.domain.precedent.dto.projection.PrecedentSummaryView;
import com.lawchat.domain.precedent.entity.Precedent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrecedentRepository extends JpaRepository<Precedent, Long> {

    Optional<Precedent> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    /**
     * 검색어(keyword)가 있을 때만 사용하는 쿼리. FULLTEXT MATCH()가 WHERE절에 항상 등장하기 때문에
     * (파라미터가 null이어도 플랜 타임에 사라지지 않는다) 키워드 없이 필터만 조합하는 경우엔
     * 절대 이 메서드를 타면 안 된다 - searchByFilters()를 대신 써야 한다 (PrecedentSearchService 참고).
     */
    @Query(
            value = """
                    SELECT
                        p.precedent_id   AS precedentId,
                        p.case_number    AS caseNumber,
                        p.case_name      AS caseName,
                        p.court_name     AS courtName,
                        p.decided_date   AS decidedDate,
                        p.summary        AS summary,
                        p.case_type_name AS caseTypeName
                    FROM precedents p
                    WHERE MATCH(p.case_name, p.holding, p.summary, p.full_text, p.referenced_articles)
                          AGAINST (CONCAT('+', :keyword, '*') IN BOOLEAN MODE)
                      AND (:categoriesEmpty = TRUE OR p.case_type_name IN (:categories))
                      AND (:caseNumber IS NULL OR p.case_number = :caseNumber)
                      AND (:caseName IS NULL OR p.case_name LIKE CONCAT('%', :caseName, '%'))
                      AND (:referencedArticles IS NULL OR p.referenced_articles LIKE CONCAT('%', :referencedArticles, '%'))
                      AND (:courtName IS NULL OR p.court_name = :courtName)
                      AND (
                            :courtTypeEmpty = TRUE
                            OR (:hasSupreme = TRUE AND p.court_name = '대법원')
                            OR (:hasHigh = TRUE
                                AND (p.court_name LIKE '%고등법원%' OR p.court_name LIKE '%고법%'))
                            OR (:hasLower = TRUE
                                AND p.court_name <> '대법원'
                                AND p.court_name NOT LIKE '%고등법원%'
                                AND p.court_name NOT LIKE '%고법%')
                          )
                      AND (:decidedDateFrom IS NULL OR p.decided_date >= :decidedDateFrom)
                      AND (:decidedDateTo IS NULL OR p.decided_date <= :decidedDateTo)
                    ORDER BY p.synced_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM precedents p
                    WHERE MATCH(p.case_name, p.holding, p.summary, p.full_text, p.referenced_articles)
                          AGAINST (CONCAT('+', :keyword, '*') IN BOOLEAN MODE)
                      AND (:categoriesEmpty = TRUE OR p.case_type_name IN (:categories))
                      AND (:caseNumber IS NULL OR p.case_number = :caseNumber)
                      AND (:caseName IS NULL OR p.case_name LIKE CONCAT('%', :caseName, '%'))
                      AND (:referencedArticles IS NULL OR p.referenced_articles LIKE CONCAT('%', :referencedArticles, '%'))
                      AND (:courtName IS NULL OR p.court_name = :courtName)
                      AND (
                            :courtTypeEmpty = TRUE
                            OR (:hasSupreme = TRUE AND p.court_name = '대법원')
                            OR (:hasHigh = TRUE
                                AND (p.court_name LIKE '%고등법원%' OR p.court_name LIKE '%고법%'))
                            OR (:hasLower = TRUE
                                AND p.court_name <> '대법원'
                                AND p.court_name NOT LIKE '%고등법원%'
                                AND p.court_name NOT LIKE '%고법%')
                          )
                      AND (:decidedDateFrom IS NULL OR p.decided_date >= :decidedDateFrom)
                      AND (:decidedDateTo IS NULL OR p.decided_date <= :decidedDateTo)
                    """,
            nativeQuery = true
    )
    Page<PrecedentSummaryView> searchSummary(@Param("keyword") String keyword,
                                              @Param("categoriesEmpty") boolean categoriesEmpty,
                                              @Param("categories") List<String> categories,
                                              @Param("caseNumber") String caseNumber,
                                              @Param("caseName") String caseName,
                                              @Param("referencedArticles") String referencedArticles,
                                              @Param("courtTypeEmpty") boolean courtTypeEmpty,
                                              @Param("hasSupreme") boolean hasSupreme,
                                              @Param("hasHigh") boolean hasHigh,
                                              @Param("hasLower") boolean hasLower,
                                              @Param("courtName") String courtName,
                                              @Param("decidedDateFrom") LocalDate decidedDateFrom,
                                              @Param("decidedDateTo") LocalDate decidedDateTo,
                                              Pageable pageable);

    /**
     * 검색어(keyword) 없이 필터(사건종류/법원종류/법원명/사건번호/기간)만 조합할 때 쓰는 쿼리.
     * searchSummary()와 달리 MATCH() AGAINST()가 WHERE절에 아예 등장하지 않는다 - 이게 핵심이다.
     * FULLTEXT 함수가 조건절에 있으면 그 파라미터가 null이어도 MySQL이 "항상 참"으로 접어주지
     * 않아서 인덱스를 못 타고 풀스캔하는 문제가 있었는데(코드 상단 이력 참고), 법원종류 체크박스가
     * 다중선택으로 바뀌면서 이 조합을 훨씬 자주 타게 됐다 - 그래서 아예 쿼리를 분리했다.
     */
    @Query(
            value = """
                    SELECT
                        p.precedent_id   AS precedentId,
                        p.case_number    AS caseNumber,
                        p.case_name      AS caseName,
                        p.court_name     AS courtName,
                        p.decided_date   AS decidedDate,
                        p.summary        AS summary,
                        p.case_type_name AS caseTypeName
                    FROM precedents p
                    WHERE (:categoriesEmpty = TRUE OR p.case_type_name IN (:categories))
                      AND (:caseNumber IS NULL OR p.case_number = :caseNumber)
                      AND (:caseName IS NULL OR p.case_name LIKE CONCAT('%', :caseName, '%'))
                      AND (:referencedArticles IS NULL OR p.referenced_articles LIKE CONCAT('%', :referencedArticles, '%'))
                      AND (:courtName IS NULL OR p.court_name = :courtName)
                      AND (
                            :courtTypeEmpty = TRUE
                            OR (:hasSupreme = TRUE AND p.court_name = '대법원')
                            OR (:hasHigh = TRUE
                                AND (p.court_name LIKE '%고등법원%' OR p.court_name LIKE '%고법%'))
                            OR (:hasLower = TRUE
                                AND p.court_name <> '대법원'
                                AND p.court_name NOT LIKE '%고등법원%'
                                AND p.court_name NOT LIKE '%고법%')
                          )
                      AND (:decidedDateFrom IS NULL OR p.decided_date >= :decidedDateFrom)
                      AND (:decidedDateTo IS NULL OR p.decided_date <= :decidedDateTo)
                    ORDER BY p.synced_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM precedents p
                    WHERE (:categoriesEmpty = TRUE OR p.case_type_name IN (:categories))
                      AND (:caseNumber IS NULL OR p.case_number = :caseNumber)
                      AND (:caseName IS NULL OR p.case_name LIKE CONCAT('%', :caseName, '%'))
                      AND (:referencedArticles IS NULL OR p.referenced_articles LIKE CONCAT('%', :referencedArticles, '%'))
                      AND (:courtName IS NULL OR p.court_name = :courtName)
                      AND (
                            :courtTypeEmpty = TRUE
                            OR (:hasSupreme = TRUE AND p.court_name = '대법원')
                            OR (:hasHigh = TRUE
                                AND (p.court_name LIKE '%고등법원%' OR p.court_name LIKE '%고법%'))
                            OR (:hasLower = TRUE
                                AND p.court_name <> '대법원'
                                AND p.court_name NOT LIKE '%고등법원%'
                                AND p.court_name NOT LIKE '%고법%')
                          )
                      AND (:decidedDateFrom IS NULL OR p.decided_date >= :decidedDateFrom)
                      AND (:decidedDateTo IS NULL OR p.decided_date <= :decidedDateTo)
                    """,
            nativeQuery = true
    )
    Page<PrecedentSummaryView> searchByFilters(@Param("categoriesEmpty") boolean categoriesEmpty,
                                                @Param("categories") List<String> categories,
                                                @Param("caseNumber") String caseNumber,
                                                @Param("caseName") String caseName,
                                                @Param("referencedArticles") String referencedArticles,
                                                @Param("courtTypeEmpty") boolean courtTypeEmpty,
                                                @Param("hasSupreme") boolean hasSupreme,
                                                @Param("hasHigh") boolean hasHigh,
                                                @Param("hasLower") boolean hasLower,
                                                @Param("courtName") String courtName,
                                                @Param("decidedDateFrom") LocalDate decidedDateFrom,
                                                @Param("decidedDateTo") LocalDate decidedDateTo,
                                                Pageable pageable);

    Page<PrecedentSummaryView> findAllBy(Pageable pageable);

    Page<PrecedentSummaryView> findByCaseTypeNameInOrderBySyncedAtDesc(Collection<String> caseTypeNames, Pageable pageable);

    @Query("SELECT DISTINCT p.courtName FROM Precedent p ORDER BY p.courtName")
    List<String> findDistinctCourtNames();
}
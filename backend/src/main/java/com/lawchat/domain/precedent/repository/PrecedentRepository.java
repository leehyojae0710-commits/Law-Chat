package com.lawchat.domain.precedent.repository;

import com.lawchat.domain.precedent.dto.projection.PrecedentSummaryView;
import com.lawchat.domain.precedent.entity.Precedent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PrecedentRepository extends JpaRepository<Precedent, Long> {

    Optional<Precedent> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    /**
     * SearchBar 키워드(사건명/판시사항/판결요지/판결전문/참조조문) + CategoryFilter(caseTypeName) +
     * 사건번호/법원종류/법원명/선고일자 범위 조건을 함께 처리한다.
     * 각 파라미터가 null이면 해당 조건은 무시된다(전부 null이면 전체 목록).
     *
     * courtType 분류 기준 (court_type_code가 "대법원"/"그 외" 2종류뿐이라 court_name 문자열로 3단계를 만든다):
     *  - "대법원"   : court_name = '대법원'
     *  - "고등법원" : court_name에 "고등법원" 또는 "고법" 포함
     *  - "하급심"   : 위 둘 다 아닌 나머지 (지방법원/지법/행정법원/군사법원 등)
     * courtType이 위 3개 값이 아니면(null 포함) 조건 자체를 적용하지 않는다 — 서비스 레이어에서 이미 검증하지만
     * 리포지토리 단에서도 방어적으로 동일하게 처리한다.
     *
     * courtName은 courtType과 별개로, 드롭다운에서 특정 법원 하나를 정확히 선택하는 용도라 정확일치로 처리한다.
     *
     * 주의: LIKE '%keyword%'는 인덱스를 타지 못해 데이터가 많아지면 느려진다.
     * 지금은 MVP라 이걸로 충분하지만, 판례 건수가 커지면 MySQL FULLTEXT 인덱스나
     * 별도 검색엔진(Elasticsearch) 도입을 검토할 것.
     */
    @Query("""
            SELECT p FROM Precedent p
            WHERE (:keyword IS NULL
                   OR p.caseName LIKE CONCAT('%', :keyword, '%')
                   OR p.holding LIKE CONCAT('%', :keyword, '%')
                   OR p.summary LIKE CONCAT('%', :keyword, '%')
                   OR p.fullText LIKE CONCAT('%', :keyword, '%')
                   OR p.referencedArticles LIKE CONCAT('%', :keyword, '%'))
              AND (:caseTypeName IS NULL OR p.caseTypeName = :caseTypeName)
              AND (:caseNumber IS NULL OR p.caseNumber = :caseNumber)
              AND (:courtName IS NULL OR p.courtName = :courtName)
              AND (
                    :courtType IS NULL
                    OR (:courtType = '대법원' AND p.courtName = '대법원')
                    OR (:courtType = '고등법원'
                        AND (p.courtName LIKE '%고등법원%' OR p.courtName LIKE '%고법%'))
                    OR (:courtType = '하급심'
                        AND p.courtName <> '대법원'
                        AND p.courtName NOT LIKE '%고등법원%'
                        AND p.courtName NOT LIKE '%고법%')
                  )
              AND (:decidedDateFrom IS NULL OR p.decidedDate >= :decidedDateFrom)
              AND (:decidedDateTo IS NULL OR p.decidedDate <= :decidedDateTo)
            """)
    Page<Precedent> search(@Param("keyword") String keyword,
                            @Param("caseTypeName") String caseTypeName,
                            @Param("caseNumber") String caseNumber,
                            @Param("courtType") String courtType,
                            @Param("courtName") String courtName,
                            @Param("decidedDateFrom") LocalDate decidedDateFrom,
                            @Param("decidedDateTo") LocalDate decidedDateTo,
                            Pageable pageable);

    /**
     * 목록/검색 응답(PrecedentSummaryResponse)이 실제로 쓰는 컬럼만 조회하는 버전.
     * 조건은 위 search()와 완전히 동일하지만 SELECT 절에 holding/fullText/referencedArticles/
     * referencedCases(LONGTEXT)를 넣지 않아서, 페이지 이동·검색 시마다 판례 전문을 통째로
     * 읽어오던 것을 피한다. 화면에서 판례 전문이 필요한 상세 조회(getDetail)는 기존
     * findById(엔티티 전체 조회)를 그대로 쓰므로 영향 없다.
     */
    @Query("""
            SELECT p.precedentId AS precedentId,
                   p.caseNumber AS caseNumber,
                   p.caseName AS caseName,
                   p.courtName AS courtName,
                   p.decidedDate AS decidedDate,
                   p.summary AS summary,
                   p.caseTypeName AS caseTypeName
            FROM Precedent p
            WHERE (:keyword IS NULL
                   OR p.caseName LIKE CONCAT('%', :keyword, '%')
                   OR p.holding LIKE CONCAT('%', :keyword, '%')
                   OR p.summary LIKE CONCAT('%', :keyword, '%')
                   OR p.fullText LIKE CONCAT('%', :keyword, '%')
                   OR p.referencedArticles LIKE CONCAT('%', :keyword, '%'))
              AND (:caseTypeName IS NULL OR p.caseTypeName = :caseTypeName)
              AND (:caseNumber IS NULL OR p.caseNumber = :caseNumber)
              AND (:courtName IS NULL OR p.courtName = :courtName)
              AND (
                    :courtType IS NULL
                    OR (:courtType = '대법원' AND p.courtName = '대법원')
                    OR (:courtType = '고등법원'
                        AND (p.courtName LIKE '%고등법원%' OR p.courtName LIKE '%고법%'))
                    OR (:courtType = '하급심'
                        AND p.courtName <> '대법원'
                        AND p.courtName NOT LIKE '%고등법원%'
                        AND p.courtName NOT LIKE '%고법%')
                  )
              AND (:decidedDateFrom IS NULL OR p.decidedDate >= :decidedDateFrom)
              AND (:decidedDateTo IS NULL OR p.decidedDate <= :decidedDateTo)
            """)
    Page<PrecedentSummaryView> searchSummary(@Param("keyword") String keyword,
                                              @Param("caseTypeName") String caseTypeName,
                                              @Param("caseNumber") String caseNumber,
                                              @Param("courtType") String courtType,
                                              @Param("courtName") String courtName,
                                              @Param("decidedDateFrom") LocalDate decidedDateFrom,
                                              @Param("decidedDateTo") LocalDate decidedDateTo,
                                              Pageable pageable);
}

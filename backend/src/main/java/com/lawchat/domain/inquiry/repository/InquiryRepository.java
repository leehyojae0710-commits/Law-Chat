package com.lawchat.domain.inquiry.repository;

import com.lawchat.domain.inquiry.entity.Inquiry;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 정렬(최신순)은 NoticeRepository 와 동일하게 @Query 에 ORDER BY 를 박지 않고
 * 서비스에서 Pageable 의 Sort 로 주입한다.
 * (@Query 의 ORDER BY 와 Pageable 의 Sort 가 동시에 걸리면 ORDER BY 가 중복 생성되어 SQL 오류)
 */
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /** 내 문의함. 본인 것만 조회하므로 작성자 정보는 필요 없다. */
    Page<Inquiry> findByUser_UserId(Long userId, Pageable pageable);

    /**
     * 관리자 목록.
     *
     * status 는 DB 컬럼이 아니라 파생 값이라 원본 컬럼 조건으로 풀어서 쓴다.
     *   answeredOnly = TRUE  -> 답변완료 (answered_at IS NOT NULL AND is_approved = 1)
     *   answeredOnly = FALSE -> 답변대기 (그 외 전부)
     *   answeredOnly = null  -> 전체
     *
     * is_approved 가 NULL 인 행도 답변대기로 잡히도록 IS NULL 조건을 함께 둔다.
     * 이걸 빠뜨리면 해당 행이 두 필터 어디에도 안 잡혀 목록에서 사라진다.
     *
     * user 를 LEFT JOIN FETCH 하는 이유:
     *   응답에 작성자 이메일이 필요한데 @ManyToOne 이 LAZY 라 그냥 두면 건별 추가 쿼리(N+1)가 나간다.
     *   탈퇴 회원은 user_id 가 NULL 이므로 반드시 LEFT (INNER 로 하면 해당 문의가 통째로 누락된다).
     */
    @Query(value = """
            SELECT i FROM Inquiry i
            LEFT JOIN FETCH i.user
            WHERE (:category IS NULL OR i.category = :category)
              AND (
                   :answeredOnly IS NULL
                OR (:answeredOnly = TRUE
                    AND i.answeredAt IS NOT NULL
                    AND i.isApproved = TRUE)
                OR (:answeredOnly = FALSE
                    AND (i.answeredAt IS NULL
                         OR i.isApproved IS NULL
                         OR i.isApproved = FALSE))
              )
            """,
            countQuery = """
            SELECT COUNT(i) FROM Inquiry i
            WHERE (:category IS NULL OR i.category = :category)
              AND (
                   :answeredOnly IS NULL
                OR (:answeredOnly = TRUE
                    AND i.answeredAt IS NOT NULL
                    AND i.isApproved = TRUE)
                OR (:answeredOnly = FALSE
                    AND (i.answeredAt IS NULL
                         OR i.isApproved IS NULL
                         OR i.isApproved = FALSE))
              )
            """)
    Page<Inquiry> searchForAdmin(@Param("answeredOnly") Boolean answeredOnly,
                                 @Param("category") InquiryCategory category,
                                 Pageable pageable);

    /** 관리자 단건 조회 시 작성자까지 한 번에 가져온다. */
    @EntityGraph(attributePaths = "user")
    Optional<Inquiry> findWithUserByInquiryId(Long inquiryId);

    /** 관리자 대시보드용 미답변 건수 */
    long countByAnsweredAtIsNull();
}

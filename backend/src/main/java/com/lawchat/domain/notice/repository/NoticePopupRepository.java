package com.lawchat.domain.notice.repository;

import com.lawchat.domain.notice.entity.NoticePopup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticePopupRepository extends JpaRepository<NoticePopup, Long> {

    /** 사용자 화면용 - 현재 노출 기간에 걸린 팝업만 */
    @Query("SELECT p FROM NoticePopup p WHERE p.startDate <= :now AND p.endDate >= :now ORDER BY p.createdAt DESC")
    List<NoticePopup> findActivePopups(@Param("now") LocalDateTime now);

    /** 관리자 화면용 - 기간과 무관하게 등록된 전체 팝업 */
    List<NoticePopup> findAllByOrderByCreatedAtDesc();
}

package com.lawchat.domain.notice.repository;

import com.lawchat.domain.notice.entity.NoticePopup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticePopupRepository extends JpaRepository<NoticePopup, Long> {

    /** 사용자 화면용 - 현재 노출 기간에 걸린 팝업만 */
    @Query("SELECT p FROM NoticePopup p WHERE p.startDate <= :now AND p.endDate >= :now ORDER BY p.createdAt DESC")
    List<NoticePopup> findActivePopups(@Param("now") LocalDateTime now);

    /** 관리자 화면용 - 기간과 무관하게 등록된 전체 팝업 */
    List<NoticePopup> findAllByOrderByCreatedAtDesc();

    /**
     * 공지에 연동된 팝업 1건.
     * 공지 하나당 팝업은 하나만 만들어지므로 단건으로 조회한다.
     * (같은 공지로 팝업이 여러 개 생기면 여기서 예외가 나므로 데이터 이상을 바로 알 수 있다)
     */
    Optional<NoticePopup> findByNotice_NoticeId(Long noticeId);

    /** 공지 연동 팝업 존재 여부. 수정 화면에서 체크박스 초기 상태를 정할 때 쓴다. */
    boolean existsByNotice_NoticeId(Long noticeId);

    /** 공지 삭제 시 연동 팝업 일괄 제거. 없으면 아무것도 지우지 않는다. */
    void deleteByNotice_NoticeId(Long noticeId);
}

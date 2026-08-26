package com.lawchat.domain.notice.repository;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 정렬(고정공지 우선 → 최신순)은 @Query 에 ORDER BY 를 박지 않고
 * 서비스에서 Pageable 의 Sort 로 주입한다.
 * (@Query 의 ORDER BY 와 Pageable 의 Sort 가 동시에 걸리면 ORDER BY 가 중복 생성되어 SQL 오류)
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByCategory(NoticeCategory category, Pageable pageable);
}

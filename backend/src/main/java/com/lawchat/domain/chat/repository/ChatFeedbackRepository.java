package com.lawchat.domain.chat.repository;

import com.lawchat.domain.chat.entity.ChatFeedbackDataset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedbackDataset, Long> {

    /**
     * 관리자 대시보드 "최근 싫어요 피드백" 목록용.
     * reason이 비어있는 행(=좋아요)은 제외하고 최신순 상위 N개만 가져온다.
     * findAll() 후 전체를 메모리에서 정렬/필터하지 않고 DB에서 바로 필요한 만큼만 조회한다
     * (chat_feedback_dataset이 커질수록 findAll() 전체 스캔은 느려지므로).
     */
    @Query("SELECT f FROM ChatFeedbackDataset f WHERE f.reason IS NOT NULL AND f.reason <> '' ORDER BY f.createdAt DESC")
    List<ChatFeedbackDataset> findRecentDislikes(Pageable pageable);
}

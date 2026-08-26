package com.lawchat.domain.chat.repository;

import com.lawchat.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // ChatHistoryList: 내 세션 목록 (최근 수정순)
    List<ChatSession> findByUser_UserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);

    // FavoritesList: 즐겨찾기한 세션만
    List<ChatSession> findByUser_UserIdAndIsFavoriteTrueAndStatusOrderByUpdatedAtDesc(Long userId, String status);

    // 소유권 검증 포함 단건 조회 (본인 세션만 접근 허용)
    Optional<ChatSession> findBySessionIdAndUser_UserId(Long sessionId, Long userId);
}

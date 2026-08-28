package com.lawchat.domain.precedent.repository;

import com.lawchat.domain.precedent.entity.PrecedentBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrecedentBookmarkRepository extends JpaRepository<PrecedentBookmark, Long> {

    List<PrecedentBookmark> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<PrecedentBookmark> findByUser_UserIdAndPrecedent_PrecedentId(Long userId, Long precedentId);

    boolean existsByUser_UserIdAndPrecedent_PrecedentId(Long userId, Long precedentId);
}
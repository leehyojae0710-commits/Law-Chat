package com.lawchat.domain.chat.repository;

import com.lawchat.domain.chat.entity.ChatFeedbackDataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedbackDataset, Long> {
}

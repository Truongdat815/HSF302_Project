package com.fpt.elearning.repository;

import com.fpt.elearning.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
}

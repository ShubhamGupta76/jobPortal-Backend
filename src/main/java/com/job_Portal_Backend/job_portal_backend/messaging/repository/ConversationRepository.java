package com.job_Portal_Backend.job_portal_backend.messaging.repository;

import com.job_Portal_Backend.job_portal_backend.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByApplicationId(Long applicationId);
    List<Conversation> findByParticipantsUserIdOrderByUpdatedAtDesc(Long userId);
}
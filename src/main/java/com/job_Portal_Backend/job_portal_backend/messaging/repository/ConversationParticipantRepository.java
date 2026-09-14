package com.job_Portal_Backend.job_portal_backend.messaging.repository;

import com.job_Portal_Backend.job_portal_backend.messaging.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);
}
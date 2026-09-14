package com.job_Portal_Backend.job_portal_backend.messaging.repository;

import com.job_Portal_Backend.job_portal_backend.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id IN "
            + "(SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.user.id = :userId) "
            + "AND m.sender.id <> :userId ORDER BY m.createdAt DESC")
    List<Message> findTop50ReceivedByUserId(@Param("userId") Long userId,
            org.springframework.data.domain.Pageable pageable);
}
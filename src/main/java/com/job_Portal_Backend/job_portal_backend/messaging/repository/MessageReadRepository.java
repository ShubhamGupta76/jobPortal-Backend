package com.job_Portal_Backend.job_portal_backend.messaging.repository;

import com.job_Portal_Backend.job_portal_backend.messaging.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {
    Optional<MessageRead> findByMessageIdAndUserId(Long messageId, Long userId);
}
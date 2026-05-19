package com.job_Portal_Backend.job_portal_backend.interview.repository;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
    List<InterviewMessage> findTop100BySessionIdOrderByCreatedAtAsc(Long sessionId);
}

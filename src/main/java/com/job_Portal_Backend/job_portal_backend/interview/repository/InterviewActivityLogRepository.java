package com.job_Portal_Backend.job_portal_backend.interview.repository;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewActivityLogRepository extends JpaRepository<InterviewActivityLog, Long> {
    List<InterviewActivityLog> findTop100BySessionIdOrderByCreatedAtDesc(Long sessionId);
}

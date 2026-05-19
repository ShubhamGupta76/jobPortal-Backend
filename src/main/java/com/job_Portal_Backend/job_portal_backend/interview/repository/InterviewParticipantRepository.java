package com.job_Portal_Backend.job_portal_backend.interview.repository;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewParticipant;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewParticipantRepository extends JpaRepository<InterviewParticipant, Long> {
    Optional<InterviewParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<InterviewParticipant> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countBySessionIdAndStatus(Long sessionId, InterviewParticipantStatus status);
}

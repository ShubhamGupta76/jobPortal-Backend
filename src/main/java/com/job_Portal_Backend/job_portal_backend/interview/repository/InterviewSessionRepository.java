package com.job_Portal_Backend.job_portal_backend.interview.repository;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    Optional<InterviewSession> findByRoomTokenAndIsDeletedFalse(String roomToken);

    Optional<InterviewSession> findByInviteTokenAndIsDeletedFalse(String inviteToken);

    List<InterviewSession> findByRecruiterIdAndIsDeletedFalseOrderByScheduledStartAtDesc(Long recruiterId);

    List<InterviewSession> findByCandidateIdAndIsDeletedFalseOrderByScheduledStartAtDesc(Long candidateId);
}

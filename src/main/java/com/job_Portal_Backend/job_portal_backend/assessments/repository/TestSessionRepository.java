package com.job_Portal_Backend.job_portal_backend.assessments.repository;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    Optional<TestSession> findBySessionToken(String sessionToken);

    List<TestSession> findByAssessmentIdAndCandidateId(Long assessmentId, Long candidateId);

    List<TestSession> findByAssessmentId(Long assessmentId);

    Optional<TestSession> findByIdAndCandidateId(Long id, Long candidateId);
}

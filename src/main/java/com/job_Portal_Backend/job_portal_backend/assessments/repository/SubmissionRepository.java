package com.job_Portal_Backend.job_portal_backend.assessments.repository;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByTestSessionId(Long testSessionId);

    Optional<Submission> findByTestSessionIdAndQuestionId(Long testSessionId, Long questionId);

    Long countByTestSessionIdAndIsCorrectTrue(Long testSessionId);

    void deleteByTestSessionIdIn(List<Long> testSessionIds);
}

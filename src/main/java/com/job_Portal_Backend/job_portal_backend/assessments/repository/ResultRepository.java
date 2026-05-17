package com.job_Portal_Backend.job_portal_backend.assessments.repository;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findByTestSessionId(Long testSessionId);

    List<Result> findByTestSessionAssessmentIdOrderByPercentageScoreDesc(Long assessmentId);

    void deleteByTestSessionIdIn(List<Long> testSessionIds);
}

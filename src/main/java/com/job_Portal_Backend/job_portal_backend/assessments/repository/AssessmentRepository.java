package com.job_Portal_Backend.job_portal_backend.assessments.repository;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByRecruiterId(Long recruiterId);

    List<Assessment> findByJobId(Long jobId);

    Optional<Assessment> findByIdAndRecruiterId(Long id, Long recruiterId);

    @Query("SELECT a FROM Assessment a WHERE a.job.id = :jobId AND a.status IN ('PUBLISHED', 'LIVE') ORDER BY a.createdAt DESC")
    List<Assessment> findAssignableAssessmentsByJobId(@Param("jobId") Long jobId);
}

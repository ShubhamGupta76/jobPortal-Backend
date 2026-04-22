package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Interview;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

        Page<Interview> findByCandidateAndIsDeletedFalse(User candidate, Pageable pageable);

        Page<Interview> findByRecruiterAndIsDeletedFalse(User recruiter, Pageable pageable);

        List<Interview> findByCandidateAndIsDeletedFalse(User candidate);

        List<Interview> findByRecruiterAndIsDeletedFalse(User recruiter);

        List<Interview> findByJobIdAndIsDeletedFalse(Long jobId);

        @Query("SELECT i FROM Interview i WHERE i.candidate = :candidate AND i.job = :job AND i.isDeleted = false")
        List<Interview> findByCandidateAndJobAndIsDeletedFalse(@Param("candidate") User candidate,
                        @Param("job") Job job);

        @Query("SELECT i FROM Interview i WHERE i.candidate = :candidate AND i.scheduledAt >= :startDate AND i.isDeleted = false ORDER BY i.scheduledAt ASC")
        List<Interview> findUpcomingInterviewsByCandidate(@Param("candidate") User candidate,
                        @Param("startDate") LocalDateTime startDate);

        @Query("SELECT i FROM Interview i WHERE i.recruiter = :recruiter AND i.scheduledAt >= :startDate AND i.isDeleted = false ORDER BY i.scheduledAt ASC")
        List<Interview> findUpcomingInterviewsByRecruiter(@Param("recruiter") User recruiter,
                        @Param("startDate") LocalDateTime startDate);

        @Query("SELECT COUNT(i) FROM Interview i WHERE i.recruiter = :recruiter AND i.scheduledAt BETWEEN :start AND :end AND i.isDeleted = false")
        long countInterviewsByRecruiterAndDateRange(@Param("recruiter") User recruiter,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT i FROM Interview i WHERE i.isDeleted = false AND i.scheduledAt < :now AND i.status = 'SCHEDULED'")
        List<Interview> findOverdueInterviews(@Param("now") LocalDateTime now);
}
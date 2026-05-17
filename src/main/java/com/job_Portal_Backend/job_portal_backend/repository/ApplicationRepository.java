package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
        List<Application> findByUserId(Long userId);

        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.isDeleted = false")
        List<Application> findByUserIdAndNotDeleted(@Param("userId") Long userId);

        List<Application> findByJobId(Long jobId);

        @Query("SELECT a FROM Application a WHERE a.job.id = :jobId AND a.isDeleted = false")
        List<Application> findByJobIdAndNotDeleted(@Param("jobId") Long jobId);

        Page<Application> findByJobRecruiterId(Long recruiterId, Pageable pageable);

        @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId AND a.isDeleted = false")
        Page<Application> findByJobRecruiterIdAndNotDeleted(@Param("recruiterId") Long recruiterId, Pageable pageable);

        @Query("SELECT a FROM Application a WHERE a.job.recruiter.id = :recruiterId AND a.isDeleted = false")
        List<Application> findAllByJobRecruiterIdAndNotDeleted(@Param("recruiterId") Long recruiterId);

        Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.job.id = :jobId AND a.isDeleted = false")
        Optional<Application> findByUserIdAndJobIdAndNotDeleted(@Param("userId") Long userId,
                        @Param("jobId") Long jobId);

        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.job.recruiter.id = :recruiterId AND a.resumePath IS NOT NULL AND a.isDeleted = false ORDER BY a.updatedAt DESC")
        List<Application> findResumeApplicationsByUserIdAndRecruiterId(@Param("userId") Long userId,
                        @Param("recruiterId") Long recruiterId);

        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.resumePath IS NOT NULL AND a.isDeleted = false ORDER BY a.updatedAt DESC")
        List<Application> findResumeApplicationsByUserId(@Param("userId") Long userId);

        @Query("SELECT a FROM Application a WHERE a.assignedAssessment.id = :assessmentId")
        List<Application> findByAssignedAssessmentId(@Param("assessmentId") Long assessmentId);

        long countByUserId(Long userId);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.user.id = :userId AND a.isDeleted = false")
        long countByUserIdAndNotDeleted(@Param("userId") Long userId);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.user.id = :userId AND a.isDeleted = false AND LOWER(a.status) = LOWER(:status)")
        long countByUserIdAndStatusIgnoreCaseAndNotDeleted(@Param("userId") Long userId,
                        @Param("status") String status);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId AND a.isDeleted = false")
        long countByJobIdAndNotDeleted(@Param("jobId") Long jobId);

        long countByJobRecruiterId(Long recruiterId);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId AND a.isDeleted = false")
        long countByJobRecruiterIdAndNotDeleted(@Param("recruiterId") Long recruiterId);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId AND LOWER(a.status) = LOWER(:status) AND a.isDeleted = false")
        long countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(@Param("recruiterId") Long recruiterId,
                        @Param("status") String status);

        List<Application> findByUserAndIsDeletedFalse(User user);
}

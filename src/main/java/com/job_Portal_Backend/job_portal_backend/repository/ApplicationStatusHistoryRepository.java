package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    List<ApplicationStatusHistory> findTop50ByApplication_UserIdOrderByCreatedAtDesc(Long userId);

    // Batch variant for listing endpoints (e.g. all applicants for a job): one query for every
    // application's timeline instead of one query per application.
    List<ApplicationStatusHistory> findByApplicationIdInOrderByCreatedAtAsc(List<Long> applicationIds);
}
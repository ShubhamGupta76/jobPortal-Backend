package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.SavedSearchDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedSearchDeliveryRepository extends JpaRepository<SavedSearchDelivery, Long> {
    boolean existsBySavedSearchIdAndJobId(Long savedSearchId, Long jobId);

    List<SavedSearchDelivery> findTop50BySavedSearch_UserIdOrderByCreatedAtDesc(Long userId);
}
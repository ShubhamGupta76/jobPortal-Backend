package com.job_Portal_Backend.job_portal_backend.savedsearch.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SavedSearchResponse {
    private Long id;
    private String name;
    private String keyword;
    private String location;
    private String skills;
    private String experienceLevel;
    private String jobType;
    private String workplaceType;
    private Double minSalary;
    private Double maxSalary;
    private String frequency;
    private Boolean enabled;
    private LocalDateTime lastMatchedAt;
    private LocalDateTime lastDeliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
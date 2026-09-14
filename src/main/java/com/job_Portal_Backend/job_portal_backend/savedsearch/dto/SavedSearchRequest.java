package com.job_Portal_Backend.job_portal_backend.savedsearch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SavedSearchRequest {
    @NotBlank(message = "Saved search name is required")
    private String name;
    private String keyword;
    private String location;
    private String skills;
    private String experienceLevel;
    private String jobType;
    private String workplaceType;
    private Double minSalary;
    private Double maxSalary;
    private String frequency = "IMMEDIATE";
    private Boolean enabled = true;
}
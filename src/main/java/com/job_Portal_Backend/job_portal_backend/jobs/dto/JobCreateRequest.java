package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Job type is required")
    private String jobType;

    @NotBlank(message = "Experience level is required")
    private String experienceLevel;

    private Double minSalary;
    private Double maxSalary;

    private String skills;

    @NotNull(message = "Company ID is required")
    private Long companyId;
}
package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;

@Data
public class JobUpdateRequest {

    private String title;
    private String description;
    private String department;
    private String location;
    private String workplaceType;
    private String jobType;
    private String experienceLevel;
    private Double minSalary;
    private Double maxSalary;
    private String skills;
    private Long companyId;
}

package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDto {
    private Long id;
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
    private String status;
    private Long applicationCount;
    private Long companyId;
    private String companyName;
    private Long recruiterId;
    private String recruiterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

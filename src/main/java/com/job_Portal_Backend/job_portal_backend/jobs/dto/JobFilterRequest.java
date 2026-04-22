package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;

@Data
public class JobFilterRequest {
    private String location;
    private Double minSalary;
    private Double maxSalary;
    private String jobType;
    private String experienceLevel;
    private String keyword; // for search
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
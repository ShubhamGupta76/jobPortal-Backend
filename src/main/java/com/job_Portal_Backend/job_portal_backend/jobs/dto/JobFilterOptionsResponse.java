package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobFilterOptionsResponse {
    private List<String> locations;
    private List<String> jobTypes;
    private List<String> experienceLevels;
    private Double minSalary;
    private Double maxSalary;
}

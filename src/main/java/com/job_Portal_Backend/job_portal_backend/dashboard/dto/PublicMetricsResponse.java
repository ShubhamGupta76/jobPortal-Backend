package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicMetricsResponse {
    private long totalJobs;
    private long totalApplications;
    private long totalCompanies;
    private long totalUsers;
}

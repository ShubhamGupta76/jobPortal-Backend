package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import lombok.Data;

import java.util.List;

@Data
public class RecruiterDashboardResponse {
    private long totalJobs;
    private long totalApplicants;
    private long shortlistedCount;
    private List<JobDto> recentJobs;
    private List<ApplicationDto> recentApplicants;
}

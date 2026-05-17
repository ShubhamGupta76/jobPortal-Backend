package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import lombok.Data;

import java.util.List;

@Data
public class RecruiterDashboardResponse {
    private String firstName;
    private String fullName;
    private long totalJobs;
    private long openPositions;
    private long totalApplicants;
    private long shortlistedCount;
    private long interviewCount;
    private long hiredCount;
    private long pendingReviews;
    private long averageTimeToHireDays;
    private double offerAcceptRate;
    private List<JobDto> recentJobs;
    private List<ApplicationDto> recentApplicants;
    private List<RecruiterJobPerformanceDto> activeJobPerformance;
}

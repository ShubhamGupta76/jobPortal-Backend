package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import lombok.Data;

import java.util.List;

@Data
public class CandidateDashboardResponse {
    private long appliedJobs;
    private long savedJobs;
    private long unreadNotifications;
    private List<ApplicationDto> recentApplications;
    private List<JobDto> savedJobListings;
}

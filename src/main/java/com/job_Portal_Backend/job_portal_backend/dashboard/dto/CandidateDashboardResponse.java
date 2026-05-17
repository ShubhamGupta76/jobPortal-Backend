package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CandidateDashboardResponse {
    private String firstName;
    private String fullName;
    private long appliedJobs;
    private long activeApplications;
    private long savedJobs;
    private long unreadNotifications;
    private long upcomingInterviews;
    private long pendingAssessments;
    private boolean resumeAvailable;
    private LocalDateTime nextInterviewAt;
    private ProfileStrengthDto profileStrength;
    private List<ApplicationDto> recentApplications;
    private List<JobDto> savedJobListings;
    private List<JobDto> recommendedJobs;
    private List<InterviewDto> upcomingInterviewSchedule;
    private List<DashboardActivityItemDto> recentActivities;
}

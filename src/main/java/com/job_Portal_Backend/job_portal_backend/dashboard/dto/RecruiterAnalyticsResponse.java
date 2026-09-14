package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RecruiterAnalyticsResponse {
    private int periodDays;
    private long jobsPosted;
    private long activeJobs;
    private long applications;
    private long applicationsThisWeek;
    private long shortlisted;
    private long assessmentsAssigned;
    private long assessmentsCompleted;
    private long interviews;
    private long hires;
    private Map<String, Long> funnel;
}
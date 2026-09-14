package com.job_Portal_Backend.job_portal_backend.dashboard.service;

import com.job_Portal_Backend.job_portal_backend.dashboard.dto.CandidateDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.ProfileStrengthDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.PublicMetricsResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterAnalyticsResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;

public interface DashboardService {
    CandidateDashboardResponse getCandidateDashboard(User user);
    RecruiterDashboardResponse getRecruiterDashboard(User user);
    RecruiterAnalyticsResponse getRecruiterAnalytics(User user, int days);
    PublicMetricsResponse getPublicMetrics();
    ProfileStrengthDto getProfileStrength(User user);
}

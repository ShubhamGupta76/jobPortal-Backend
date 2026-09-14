package com.job_Portal_Backend.job_portal_backend.profileanalytics.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProfileAnalyticsResponse {
    private int profileCompleteness;
    private String profileCompletenessLevel;
    private String profileCompletenessMessage;

    private long profileViewsTotal;
    private long profileViewsLast30Days;
    private long recruiterViewersLast30Days;
    private long resumeViewsTotal;

    private long applicationsTotal;
    private long applicationsRespondedTotal;
    private double responseRatePercentage;
    private long interviewsTotal;
    private double interviewRatePercentage;

    private Map<String, Long> matchScoreDistribution;
    private List<TopSkillDto> topSkillsInMatchedJobs;
    private Map<String, Long> applicationsByMonth;

    private List<String> improvementSuggestions;

    @Data
    public static class TopSkillDto {
        private String skill;
        private long matchedJobCount;
    }
}

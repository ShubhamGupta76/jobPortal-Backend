package com.job_Portal_Backend.job_portal_backend.profileanalytics.service;

import com.job_Portal_Backend.job_portal_backend.dashboard.dto.ProfileStrengthDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.service.DashboardService;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.dto.ProfileAnalyticsResponse;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ProfileViewEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileAnalyticsService {

    private final DashboardService dashboardService;
    private final ProfileViewEventRepository profileViewEventRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public ProfileAnalyticsService(DashboardService dashboardService,
            ProfileViewEventRepository profileViewEventRepository,
            ApplicationRepository applicationRepository,
            JobRepository jobRepository,
            JobMapper jobMapper) {
        this.dashboardService = dashboardService;
        this.profileViewEventRepository = profileViewEventRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
    }

    public ProfileAnalyticsResponse getAnalytics(User user) {
        ProfileAnalyticsResponse response = new ProfileAnalyticsResponse();

        ProfileStrengthDto strength = dashboardService.getProfileStrength(user);
        response.setProfileCompleteness(strength.getCompletionPercentage());
        response.setProfileCompletenessLevel(strength.getLevel());
        response.setProfileCompletenessMessage(strength.getMessage());

        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        response.setProfileViewsTotal(profileViewEventRepository.countByViewedUserId(user.getId()));
        response.setProfileViewsLast30Days(
                profileViewEventRepository.countByViewedUserIdAndCreatedAtAfter(user.getId(), last30Days));
        response.setRecruiterViewersLast30Days(
                profileViewEventRepository.countDistinctViewersSince(user.getId(), last30Days));
        response.setResumeViewsTotal(profileViewEventRepository.countByViewedUserId(user.getId()));

        List<Application> applications = applicationRepository.findByUserIdAndNotDeleted(user.getId());
        long total = applications.size();
        long responded = applications.stream()
                .filter(app -> app.getStatus() != Application.ApplicationStatus.APPLIED)
                .count();
        long interviewOrBeyond = applications.stream()
                .filter(app -> app.getStatus() == Application.ApplicationStatus.INTERVIEW
                        || app.getStatus() == Application.ApplicationStatus.HIRED)
                .count();

        response.setApplicationsTotal(total);
        response.setApplicationsRespondedTotal(responded);
        response.setResponseRatePercentage(total == 0 ? 0 : round((responded * 100.0) / total));
        response.setInterviewsTotal(interviewOrBeyond);
        response.setInterviewRatePercentage(total == 0 ? 0 : round((interviewOrBeyond * 100.0) / total));

        response.setApplicationsByMonth(buildApplicationsByMonth(applications));

        List<JobDto> scoredJobs = jobRepository.findJobsWithFilters(null, null, null, null, null, null)
                .stream()
                .map(job -> jobMapper.toDto(job, user))
                .toList();
        response.setMatchScoreDistribution(buildMatchDistribution(scoredJobs));
        response.setTopSkillsInMatchedJobs(buildTopSkills(scoredJobs));
        response.setImprovementSuggestions(buildSuggestions(user, strength, scoredJobs));

        return response;
    }

    private Map<String, Long> buildApplicationsByMonth(List<Application> applications) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> byMonth = new LinkedHashMap<>();
        LocalDateTime cursor = LocalDateTime.now().minusMonths(5).withDayOfMonth(1);
        for (int i = 0; i < 6; i++) {
            byMonth.put(cursor.format(formatter), 0L);
            cursor = cursor.plusMonths(1);
        }
        for (Application application : applications) {
            if (application.getCreatedAt() == null) continue;
            String key = application.getCreatedAt().format(formatter);
            if (byMonth.containsKey(key)) {
                byMonth.merge(key, 1L, Long::sum);
            }
        }
        return byMonth;
    }

    private Map<String, Long> buildMatchDistribution(List<JobDto> scoredJobs) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("80-100", 0L);
        distribution.put("60-79", 0L);
        distribution.put("40-59", 0L);
        distribution.put("0-39", 0L);
        for (JobDto job : scoredJobs) {
            int score = job.getMatchScore() == null ? 0 : job.getMatchScore();
            String bucket = score >= 80 ? "80-100" : score >= 60 ? "60-79" : score >= 40 ? "40-59" : "0-39";
            distribution.merge(bucket, 1L, Long::sum);
        }
        return distribution;
    }

    private List<ProfileAnalyticsResponse.TopSkillDto> buildTopSkills(List<JobDto> scoredJobs) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (JobDto job : scoredJobs) {
            if (job.getMatchScore() == null || job.getMatchScore() < 60 || job.getMatchingSkills() == null) {
                continue;
            }
            for (String skill : job.getMatchingSkills()) {
                if (skill == null || skill.isBlank()) continue;
                counts.merge(skill.trim(), 1L, Long::sum);
            }
        }
        List<ProfileAnalyticsResponse.TopSkillDto> result = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    ProfileAnalyticsResponse.TopSkillDto dto = new ProfileAnalyticsResponse.TopSkillDto();
                    dto.setSkill(entry.getKey());
                    dto.setMatchedJobCount(entry.getValue());
                    result.add(dto);
                });
        return result;
    }

    private List<String> buildSuggestions(User user, ProfileStrengthDto strength, List<JobDto> scoredJobs) {
        List<String> suggestions = new ArrayList<>();
        if (strength.getCompletionPercentage() < 80) {
            suggestions.add(strength.getMessage());
        }
        Map<String, Long> missingSkillCounts = new LinkedHashMap<>();
        for (JobDto job : scoredJobs) {
            if (job.getMissingSkills() == null) continue;
            for (String skill : job.getMissingSkills()) {
                if (skill == null || skill.isBlank()) continue;
                missingSkillCounts.merge(skill.trim(), 1L, Long::sum);
            }
        }
        missingSkillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> suggestions.add(
                        "Adding \"" + entry.getKey() + "\" to your skills could improve your match on " + entry.getValue() + " open role(s)."));
        if (suggestions.isEmpty()) {
            suggestions.add("Your profile is in good shape. Keep applying to strong matches.");
        }
        return suggestions;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

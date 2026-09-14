package com.job_Portal_Backend.job_portal_backend.profileanalytics.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.dto.ActivityEventDto;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.dto.ProfileAnalyticsResponse;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.service.CandidateActivityService;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.service.ProfileAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidate")
@PreAuthorize("hasRole('USER')")
public class CandidateInsightsController {

    private final ProfileAnalyticsService profileAnalyticsService;
    private final CandidateActivityService candidateActivityService;

    public CandidateInsightsController(ProfileAnalyticsService profileAnalyticsService,
            CandidateActivityService candidateActivityService) {
        this.profileAnalyticsService = profileAnalyticsService;
        this.candidateActivityService = candidateActivityService;
    }

    @GetMapping("/profile/analytics")
    public ResponseEntity<ApiResponse<ProfileAnalyticsResponse>> getProfileAnalytics(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile analytics retrieved successfully",
                profileAnalyticsService.getAnalytics(user)));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityEventDto>>> getActivity(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 50));
        return ResponseEntity.ok(new ApiResponse<>(true, "Activity feed retrieved successfully",
                candidateActivityService.getActivity(user, type, Math.max(page, 0), boundedSize)));
    }
}

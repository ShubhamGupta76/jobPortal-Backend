package com.job_Portal_Backend.job_portal_backend.recommendations.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.recommendations.dto.RecommendedJobDto;
import com.job_Portal_Backend.job_portal_backend.recommendations.service.RecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidate/recommendations")
@PreAuthorize("hasRole('USER')")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecommendedJobDto>>> getRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer minScore) {
        int boundedSize = Math.max(1, Math.min(size, 50));
        return ResponseEntity.ok(new ApiResponse<>(true, "Recommendations retrieved successfully",
                recommendationService.getRecommendations(user, Math.max(page, 0), boundedSize, minScore)));
    }

    @PostMapping("/{jobId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable Long jobId, @AuthenticationPrincipal User user) {
        recommendationService.dismiss(user, jobId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Job dismissed from recommendations", null));
    }

    @DeleteMapping("/{jobId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> undoDismiss(@PathVariable Long jobId, @AuthenticationPrincipal User user) {
        recommendationService.undoDismiss(user, jobId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dismissal undone", null));
    }
}

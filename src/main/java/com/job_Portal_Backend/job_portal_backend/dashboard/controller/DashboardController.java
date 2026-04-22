package com.job_Portal_Backend.job_portal_backend.dashboard.controller;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.CandidateDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.PublicMetricsResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.service.DashboardService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService, JwtService jwtService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping("/candidate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CandidateDashboardResponse>> getCandidateDashboard(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Candidate dashboard retrieved successfully", dashboardService.getCandidateDashboard(resolveUser(token))));
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<RecruiterDashboardResponse>> getRecruiterDashboard(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Recruiter dashboard retrieved successfully", dashboardService.getRecruiterDashboard(resolveUser(token))));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicMetricsResponse>> getPublicMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Public metrics retrieved successfully", dashboardService.getPublicMetrics()));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

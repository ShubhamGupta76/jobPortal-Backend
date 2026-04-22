package com.job_Portal_Backend.job_portal_backend.applications.controller;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationCreateRequest;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationStatusUpdateRequest;
import com.job_Portal_Backend.job_portal_backend.applications.service.ApplicationService;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> applyToJob(
            @ModelAttribute ApplicationCreateRequest request,
            @RequestHeader("Authorization") String token) throws IOException {
        String email = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ApplicationDto application = applicationService.applyToJob(request, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Application submitted successfully", application));
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getMyApplications(
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ApplicationDto> applications = applicationService.getApplicationsByUser(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Applications retrieved successfully", applications));
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Page<ApplicationDto>>> getApplicationsByRecruiter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<ApplicationDto> applications = applicationService.getApplicationsByRecruiter(recruiter, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Applications retrieved successfully", applications));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> getApplicationsByJob(
            @PathVariable Long jobId,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ApplicationDto> applications = applicationService.getApplicationsByJob(jobId, recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Applications retrieved successfully", applications));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ApplicationDto application = applicationService.updateApplicationStatus(id, request.getStatus(), recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Application status updated successfully", application));
    }
}

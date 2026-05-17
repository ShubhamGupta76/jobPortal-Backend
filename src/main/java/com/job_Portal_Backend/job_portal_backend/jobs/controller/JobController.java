package com.job_Portal_Backend.job_portal_backend.jobs.controller;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobCreateRequest;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobFilterOptionsResponse;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobFilterRequest;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.SearchSuggestionsResponse;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobUpdateRequest;
import com.job_Portal_Backend.job_portal_backend.jobs.service.JobService;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDto>>> getJobs(JobFilterRequest filter) {
        List<JobDto> jobs = jobService.getJobs(filter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDto>> getJobById(@PathVariable Long id) {
        JobDto job = jobService.getJobById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Job retrieved successfully", job));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionsResponse>> getSuggestions(@RequestParam String keyword) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Search suggestions retrieved successfully", jobService.getSearchSuggestions(keyword)));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<JobFilterOptionsResponse>> getFilterOptions() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Filter options retrieved successfully", jobService.getFilterOptions()));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobDto>> createJob(@Valid @RequestBody JobCreateRequest request,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JobDto job = jobService.createJob(request, recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Job created successfully", job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobDto>> updateJob(@PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JobDto job = jobService.updateJob(id, request, recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Job updated successfully", job));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        jobService.deleteJob(id, recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Job deleted successfully", null));
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobDto>>> getMyJobs(@RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<JobDto> jobs = jobService.getJobsByRecruiter(recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Jobs retrieved successfully", jobs));
    }

    @PostMapping("/archive-old")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Integer>> archiveOldJobs(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "90") int days) {
        String email = jwtService.extractUsername(token.substring(7));
        User recruiter = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int archivedCount = jobService.archiveOldJobs(recruiter, days);
        return ResponseEntity.ok(new ApiResponse<>(true, "Old jobs archived successfully", archivedCount));
    }
}

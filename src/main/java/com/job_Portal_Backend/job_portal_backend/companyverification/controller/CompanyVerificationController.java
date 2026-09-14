package com.job_Portal_Backend.job_portal_backend.companyverification.controller;

import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationRequest;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationResponse;
import com.job_Portal_Backend.job_portal_backend.companyverification.service.CompanyVerificationService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/verification")
public class CompanyVerificationController {

    private final CompanyVerificationService verificationService;

    public CompanyVerificationController(CompanyVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> submit(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyVerificationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification request submitted",
                verificationService.submit(companyId, request, user)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> get(
            @PathVariable Long companyId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification status retrieved",
                verificationService.getForCompany(companyId, user)));
    }
}

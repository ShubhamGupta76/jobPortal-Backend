package com.job_Portal_Backend.job_portal_backend.reports.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportDto;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportRequest;
import com.job_Portal_Backend.job_portal_backend.reports.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'RECRUITER')")
    public ResponseEntity<ApiResponse<ReportDto>> create(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Report submitted successfully", reportService.create(request, user)));
    }
}

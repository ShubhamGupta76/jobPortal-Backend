package com.job_Portal_Backend.job_portal_backend.reports.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportDecisionRequest;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportDto;
import com.job_Portal_Backend.job_portal_backend.reports.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReportDto>>> list(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reports retrieved successfully",
                reportService.adminList(status, targetType, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Report retrieved successfully", reportService.adminGet(id)));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ReportDto>> markUnderReview(
            @PathVariable Long id, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Report marked under review",
                reportService.markUnderReview(id, admin)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ReportDto>> resolve(
            @PathVariable Long id,
            @RequestBody(required = false) ReportDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        String note = request != null ? request.getNote() : null;
        boolean suspend = request != null && request.isSuspendTarget();
        return ResponseEntity.ok(new ApiResponse<>(true, "Report resolved",
                reportService.resolve(id, note, suspend, admin)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ReportDto>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ReportDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(new ApiResponse<>(true, "Report rejected",
                reportService.reject(id, note, admin)));
    }
}

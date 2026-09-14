package com.job_Portal_Backend.job_portal_backend.companyverification.controller;

import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationDecisionRequest;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationResponse;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationSummaryDto;
import com.job_Portal_Backend.job_portal_backend.companyverification.service.CompanyVerificationService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/company-verifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyVerificationController {

    private final CompanyVerificationService verificationService;
    private final FileUploadService fileUploadService;

    public AdminCompanyVerificationController(CompanyVerificationService verificationService,
            FileUploadService fileUploadService) {
        this.verificationService = verificationService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompanyVerificationSummaryDto>>> getQueue(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification queue retrieved",
                verificationService.getQueue(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification detail retrieved",
                verificationService.getDetail(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) CompanyVerificationDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        String note = request != null ? request.getNote() : null;
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification approved",
                verificationService.approve(id, note, admin)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> reject(
            @PathVariable Long id,
            @RequestBody CompanyVerificationDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification rejected",
                verificationService.reject(id, request.getNote(), admin)));
    }

    @PostMapping("/{id}/request-info")
    public ResponseEntity<ApiResponse<CompanyVerificationResponse>> requestInfo(
            @PathVariable Long id,
            @RequestBody CompanyVerificationDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Additional information requested",
                verificationService.requestMoreInfo(id, request.getNote(), admin)));
    }

    @GetMapping("/documents/{fileId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(@PathVariable Long fileId)
            throws IOException {
        byte[] fileData = fileUploadService.downloadFileForAdmin(fileId);
        var fileInfo = fileUploadService.getFileByIdForAdmin(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                .body(new org.springframework.core.io.ByteArrayResource(fileData));
    }
}

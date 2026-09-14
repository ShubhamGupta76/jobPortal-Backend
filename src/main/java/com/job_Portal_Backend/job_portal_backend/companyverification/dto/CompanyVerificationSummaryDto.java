package com.job_Portal_Backend.job_portal_backend.companyverification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyVerificationSummaryDto {
    private Long id;
    private Long companyId;
    private String companyName;
    private String status;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}

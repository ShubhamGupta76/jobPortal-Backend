package com.job_Portal_Backend.job_portal_backend.companyverification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyVerificationHistoryDto {
    private String status;
    private String actorName;
    private String note;
    private LocalDateTime createdAt;
}

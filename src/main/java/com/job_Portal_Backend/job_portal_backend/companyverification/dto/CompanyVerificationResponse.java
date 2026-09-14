package com.job_Portal_Backend.job_portal_backend.companyverification.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompanyVerificationResponse {
    private Long id;
    private Long companyId;
    private String companyName;
    private String status;

    private String legalName;
    private String registrationNumber;
    private String companyType;
    private String officialWebsite;
    private String officialEmailDomain;

    private String submittedByName;
    private LocalDateTime submittedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String latestNote;

    private List<CompanyVerificationDocumentDto> documents;
    private List<CompanyVerificationHistoryDto> history;
}

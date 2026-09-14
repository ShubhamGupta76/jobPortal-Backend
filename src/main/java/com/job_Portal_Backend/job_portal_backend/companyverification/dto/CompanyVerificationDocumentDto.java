package com.job_Portal_Backend.job_portal_backend.companyverification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyVerificationDocumentDto {
    private Long id;
    private Long fileId;
    private String filename;
    private String contentType;
    private Long fileSize;
    private String downloadUrl;
    private String label;
    private LocalDateTime attachedAt;
}

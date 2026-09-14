package com.job_Portal_Backend.job_portal_backend.resumelibrary.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeDocumentDto {
    private Long id;
    private String label;
    private String filename;
    private String contentType;
    private Long fileSize;
    private boolean primary;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}

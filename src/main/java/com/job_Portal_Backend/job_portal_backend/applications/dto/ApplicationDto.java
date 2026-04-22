package com.job_Portal_Backend.job_portal_backend.applications.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationDto {
    private Long id;
    private Long userId;
    private String userName;
    private Long jobId;
    private String jobTitle;
    private String status;
    private Long assessmentId;
    private String assessmentTitle;
    private String resumePath;
    private String resumeDownloadUrl;
    private String coverLetter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

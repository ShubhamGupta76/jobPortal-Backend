package com.job_Portal_Backend.job_portal_backend.applications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ApplicationCreateRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String coverLetter;

    private MultipartFile resume;
}
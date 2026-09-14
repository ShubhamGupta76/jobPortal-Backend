package com.job_Portal_Backend.job_portal_backend.resumelibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeRenameRequest {

    @NotBlank(message = "Resume name is required")
    private String label;
}

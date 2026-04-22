package com.job_Portal_Backend.job_portal_backend.companies.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {
    @NotBlank(message = "Company name is required")
    private String name;

    private String description;
    private String website;
    private String location;
    private String industry;
    private String size;
}

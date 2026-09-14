package com.job_Portal_Backend.job_portal_backend.companyverification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CompanyVerificationRequest {

    @NotBlank(message = "Legal company name is required")
    private String legalName;

    private String registrationNumber;
    private String companyType;
    private String officialWebsite;
    private String officialEmailDomain;

    private List<Long> documentFileIds;
}

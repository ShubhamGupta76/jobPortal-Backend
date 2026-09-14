package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BillingPlanDto {
    private String code;
    private String displayName;
    private String currency;
    private Long monthlyPriceMinor;
    private Long annualPriceMinor;
    private Integer maxActiveJobs;
    private Integer includedCredits;
    private Integer featuredJobAllowance;
}

package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BillingSummaryDto {
    private SubscriptionDto subscription;
    private int creditBalance;
    private int lowCreditThreshold;
    private Integer maxActiveJobs;
    private long activeJobCount;
    private String viewerRole;
}

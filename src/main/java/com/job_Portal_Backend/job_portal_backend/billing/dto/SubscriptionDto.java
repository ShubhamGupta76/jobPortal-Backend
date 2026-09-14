package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SubscriptionDto {
    private String planCode;
    private String planDisplayName;
    private String status;
    private String provider;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
}

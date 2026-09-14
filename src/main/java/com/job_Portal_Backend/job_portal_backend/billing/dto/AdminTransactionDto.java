package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminTransactionDto {
    private Long id;
    private Long companyId;
    private String companyName;
    private String purpose;
    private String planCode;
    private Long amountMinor;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}

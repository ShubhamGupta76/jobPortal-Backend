package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentTransactionDto {
    private Long id;
    private String purpose;
    private String planCode;
    private String provider;
    private Long amountMinor;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}

package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CreditLedgerEntryDto {
    private Long id;
    private Integer amount;
    private String type;
    private String description;
    private Integer balanceAfter;
    private LocalDateTime createdAt;
}

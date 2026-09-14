package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private String invoiceNumber;
    private Long amountMinor;
    private String currency;
    private String status;
    private LocalDateTime issueDate;
    private LocalDateTime paidDate;
    private String providerInvoiceUrl;
}

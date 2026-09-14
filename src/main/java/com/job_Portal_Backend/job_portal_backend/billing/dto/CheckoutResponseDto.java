package com.job_Portal_Backend.job_portal_backend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponseDto {
    private Long transactionId;
    private String status;
    private String checkoutUrl;
    private boolean requiresManualConfirmation;
    private String message;
}

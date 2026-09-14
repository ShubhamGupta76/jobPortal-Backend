package com.job_Portal_Backend.job_portal_backend.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminConfirmPaymentRequest {
    @NotBlank
    private String reason;
}

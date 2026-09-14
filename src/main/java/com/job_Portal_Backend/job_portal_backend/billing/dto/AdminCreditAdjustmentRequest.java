package com.job_Portal_Backend.job_portal_backend.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreditAdjustmentRequest {
    @NotNull
    private Integer amount;

    @NotBlank
    private String reason;
}

package com.job_Portal_Backend.job_portal_backend.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * The client selects a plan by CODE and a billing cycle only — never an amount. The backend
 * looks up the plan's authoritative price server-side (see BillingService.checkout); any
 * amount/price field submitted here would be ignored even if present, so none is defined.
 */
@Data
public class CheckoutRequest {

    @NotBlank
    private String planCode;

    // "MONTHLY" or "ANNUAL"; defaults to MONTHLY if omitted.
    private String billingCycle;

    // Optional client-generated idempotency key to safely retry a checkout call.
    private String idempotencyKey;
}

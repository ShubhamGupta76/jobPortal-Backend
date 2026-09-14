package com.job_Portal_Backend.job_portal_backend.billing.controller;

import com.job_Portal_Backend.job_portal_backend.billing.service.BillingWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * External payment-provider callback. Unauthenticated by JWT (a provider cannot supply one) —
 * authenticity instead comes entirely from {@link BillingWebhookService#handle} verifying the
 * provider's signature over the raw request body. See SecurityConfig for why this path is
 * permitAll, and ManualPaymentProvider for the signature scheme actually enforced.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class BillingWebhookController {

    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final BillingWebhookService webhookService;

    public BillingWebhookController(BillingWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(@PathVariable String provider, @RequestBody String rawPayload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) {
        webhookService.handle(provider, rawPayload, signature);
        return ResponseEntity.ok().build();
    }
}

package com.job_Portal_Backend.job_portal_backend.billing.provider;

import com.job_Portal_Backend.job_portal_backend.entity.BillingPlan;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;

/**
 * Abstraction over "however we actually collect money." No implementation of this interface may
 * mark a transaction SUCCEEDED as a side effect of creating a checkout session — only a verified
 * webhook (verifyWebhook + the caller acting on its result) or an explicitly audited admin action
 * may do that. See ManualPaymentProvider for the implementation active while no real gateway
 * (e.g. Stripe) is configured in this environment.
 */
public interface PaymentProvider {

    String getProviderName();

    /**
     * Creates (and persists as PENDING) a transaction for the given plan/company, returning a
     * result the frontend can act on. Never returns a "success" state itself.
     */
    CheckoutResult createCheckoutSession(Company company, BillingPlan plan, PaymentTransaction transaction);

    /**
     * Verifies an inbound webhook's signature and parses it into a provider-agnostic event. Throws
     * if the signature is invalid; callers must reject the request on any exception here rather
     * than falling back to processing an unverified payload.
     */
    WebhookEvent verifyWebhook(String rawPayload, String signatureHeader);

    record CheckoutResult(String checkoutUrl, boolean requiresManualConfirmation, String message) {
    }

    record WebhookEvent(String providerEventId, String eventType, String providerTransactionId, String status) {
    }
}

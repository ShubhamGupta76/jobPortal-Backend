package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.billing.provider.ManualPaymentProvider;
import com.job_Portal_Backend.job_portal_backend.billing.provider.PaymentProvider;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import com.job_Portal_Backend.job_portal_backend.repository.PaymentTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Processes provider webhooks. Provider webhooks are the ONLY automatic path by which a
 * transaction becomes SUCCEEDED/FAILED (the other, non-automatic path is an audited admin
 * confirmation — see BillingAdminService); nothing here ever trusts a status the caller merely
 * asserts without a verified signature.
 *
 * <p>Idempotency: every accepted (signature-valid) event is recorded via
 * {@link WebhookEventRecorder#recordIfNew} under a unique (provider, providerEventId) constraint,
 * in its own transaction, BEFORE any business effect is applied. A duplicate delivery hits that
 * constraint and is rejected there, so a provider's "at least once" delivery guarantee can never
 * double-grant credits or double-activate a subscription. This method itself intentionally holds
 * no single database transaction across the whole call — each step below commits independently,
 * which is what keeps a business-logic failure from also rolling back the fact that the event was
 * already recorded (that would defeat the idempotency guard on the provider's automatic retry).
 */
@Service
public class BillingWebhookService {

    private final ManualPaymentProvider paymentProvider;
    private final PaymentTransactionRepository transactionRepository;
    private final BillingService billingService;
    private final WebhookEventRecorder webhookEventRecorder;

    public BillingWebhookService(ManualPaymentProvider paymentProvider,
            PaymentTransactionRepository transactionRepository,
            BillingService billingService,
            WebhookEventRecorder webhookEventRecorder) {
        this.paymentProvider = paymentProvider;
        this.transactionRepository = transactionRepository;
        this.billingService = billingService;
        this.webhookEventRecorder = webhookEventRecorder;
    }

    public void handle(String providerPathSegment, String rawPayload, String signatureHeader) {
        if (!paymentProvider.getProviderName().equalsIgnoreCase(providerPathSegment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown payment provider");
        }

        PaymentProvider.WebhookEvent event = paymentProvider.verifyWebhook(rawPayload, signatureHeader);

        Optional<Long> recordId = webhookEventRecorder.recordIfNew(
                paymentProvider.getProviderName(), event.providerEventId(), event.eventType(), rawPayload);
        if (recordId.isEmpty()) {
            // Already processed this exact provider event id before; accept silently (idempotent).
            return;
        }

        try {
            PaymentTransaction transaction = transactionRepository
                    .findByProviderAndProviderTransactionId(paymentProvider.getProviderName(), event.providerTransactionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Webhook references an unknown transaction"));

            if ("SUCCEEDED".equalsIgnoreCase(event.status())) {
                billingService.applySuccessfulPayment(transaction.getId());
            } else if ("FAILED".equalsIgnoreCase(event.status())) {
                billingService.applyFailedPayment(transaction.getId());
            }

            webhookEventRecorder.markProcessed(recordId.get());
        } catch (RuntimeException e) {
            webhookEventRecorder.markFailed(recordId.get(), e.getMessage());
            throw e;
        }
    }
}

package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.PaymentWebhookEvent;
import com.job_Portal_Backend.job_portal_backend.repository.PaymentWebhookEventRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Isolated into its own bean so the REQUIRES_NEW transactions below actually go through Spring's
 * proxy (a self-invoked @Transactional method on the same class is silently not proxied). Each
 * method commits or rolls back independently of whatever transaction the caller (BillingWebhookService)
 * is in, which matters specifically for {@link #recordIfNew}: a duplicate provider event id
 * triggers a database constraint violation that would otherwise abort the whole surrounding
 * transaction (Postgres aborts the entire transaction on a constraint violation, not just the
 * failing statement) — here it only rolls back this one small insert.
 */
@Service
class WebhookEventRecorder {

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final AuditLogService auditLogService;

    WebhookEventRecorder(PaymentWebhookEventRepository webhookEventRepository, AuditLogService auditLogService) {
        this.webhookEventRepository = webhookEventRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<Long> recordIfNew(String provider, String providerEventId, String eventType, String rawPayload) {
        PaymentWebhookEvent record = new PaymentWebhookEvent();
        record.setProvider(provider);
        record.setProviderEventId(providerEventId);
        record.setEventType(eventType);
        record.setPayload(rawPayload);
        record.setStatus(PaymentWebhookEvent.Status.RECEIVED);
        try {
            record = webhookEventRepository.save(record);
            return Optional.of(record.getId());
        } catch (DataIntegrityViolationException duplicate) {
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markProcessed(Long id) {
        webhookEventRepository.findById(id).ifPresent(record -> {
            record.setStatus(PaymentWebhookEvent.Status.PROCESSED);
            record.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(Long id, String reason) {
        webhookEventRepository.findById(id).ifPresent(record -> {
            record.setStatus(PaymentWebhookEvent.Status.FAILED);
            webhookEventRepository.save(record);
        });
        auditLogService.logAction("PaymentWebhookEvent", id, "WEBHOOK_PROCESSING_FAILED", null,
                "Webhook processing failed: " + reason, null, null);
    }
}

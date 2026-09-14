package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Records every webhook delivery this app has accepted (valid signature), keyed on
 * (provider, providerEventId), so a duplicate delivery of the same event — which every payment
 * provider's docs say to expect — is rejected at the database level before it can double-process
 * a payment. {@code payload} stores the provider's event body for audit/debugging; providers
 * already redact card data from webhook payloads, so this is never a place raw card data could
 * end up.
 */
@Entity
@Table(name = "payment_webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_provider_event", columnNames = { "provider", "provider_event_id" })
})
@Getter
@Setter
@ToString(exclude = "payload")
public class PaymentWebhookEvent {

    public enum Status {
        RECEIVED,
        PROCESSED,
        IGNORED_DUPLICATE,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 150)
    private String providerEventId;

    @Column(name = "event_type", length = 60)
    private String eventType;

    @Lob
    @Column
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

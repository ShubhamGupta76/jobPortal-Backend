package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One attempted payment. The amount/currency are always copied from BillingPlan (or a credit
 * package) server-side at creation time — never accepted from the client — and status only ever
 * moves to SUCCEEDED through the webhook or an audited admin confirmation, never a client call.
 */
@Entity
@Table(name = "payment_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_transaction_provider_ref", columnNames = { "provider", "provider_transaction_id" }),
        @UniqueConstraint(name = "uk_payment_transaction_idempotency_key", columnNames = "idempotency_key")
}, indexes = {
        @Index(name = "idx_payment_transactions_company_id", columnList = "company_id")
})
@Getter
@Setter
@ToString(exclude = { "company", "subscription" })
public class PaymentTransaction {

    public enum Status {
        PENDING,
        SUCCEEDED,
        FAILED,
        REFUNDED,
        CANCELED
    }

    public enum Purpose {
        SUBSCRIPTION,
        CREDIT_PURCHASE
    }

    public enum BillingCycle {
        MONTHLY,
        ANNUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private CompanySubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private BillingPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 10)
    private BillingCycle billingCycle;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_transaction_id", length = 120)
    private String providerTransactionId;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    // Credits this transaction grants once SUCCEEDED (credit-purchase transactions).
    @Column(name = "credits_granted")
    private Integer creditsGranted;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

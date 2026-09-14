package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * The company's current subscription state. One mutable row per company (unique on company_id) —
 * transitions are recorded, not a growing history table, since the credit ledger and audit log
 * already capture the history of what happened and when.
 *
 * <p>A company with no row here is treated as being on the FREE plan by
 * SubscriptionEntitlementService (lazy default), exactly like NotificationPreference's
 * "absence is meaningful" pattern — so no migration/backfill is needed for existing companies.
 */
@Entity
@Table(name = "company_subscriptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_subscription_company", columnNames = "company_id")
})
@Getter
@Setter
@ToString(exclude = "company")
public class CompanySubscription {

    public enum Status {
        TRIALING,
        ACTIVE,
        PAST_DUE,
        PAUSED,
        CANCELED,
        EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private BillingPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    // "MANUAL" until a real gateway (e.g. Stripe) is configured; see PaymentProvider.
    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_customer_id", length = 120)
    private String providerCustomerId;

    @Column(name = "provider_subscription_id", length = 120)
    private String providerSubscriptionId;

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = false;

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

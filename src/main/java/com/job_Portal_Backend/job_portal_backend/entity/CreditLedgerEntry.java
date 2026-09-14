package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One immutable entry in a company's credit ledger. Rows are never updated or deleted — a
 * company's balance is the sum of its entries' {@code amount} (positive = grant/purchase,
 * negative = consumption), computed inside the same row-locked transaction that appends a new
 * entry (see CreditLedgerService), so it can never go negative or double-count under concurrency.
 * {@code balanceAfter} is a convenience snapshot for fast reads/display, not a second source of
 * truth.
 */
@Entity
@Table(name = "credit_ledger_entries", indexes = {
        @Index(name = "idx_credit_ledger_company_id", columnList = "company_id")
})
@Getter
@Setter
@ToString(exclude = { "company", "createdBy" })
public class CreditLedgerEntry {

    public enum Type {
        PURCHASE,
        SUBSCRIPTION_GRANT,
        JOB_POST,
        FEATURED_JOB,
        REFUND,
        ADMIN_ADJUSTMENT,
        EXPIRATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Type type;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 255)
    private String description;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

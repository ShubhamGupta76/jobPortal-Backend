package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One invoice per successful PaymentTransaction. Never generated for a PENDING/FAILED
 * transaction, and never marked PAID except by the same authoritative flow (webhook or audited
 * admin confirmation) that marks its transaction SUCCEEDED.
 */
@Entity
@Table(name = "invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_number", columnNames = "invoice_number"),
        @UniqueConstraint(name = "uk_invoice_transaction", columnNames = "transaction_id")
}, indexes = {
        @Index(name = "idx_invoices_company_id", columnList = "company_id")
})
@Getter
@Setter
@ToString(exclude = { "company", "transaction" })
public class Invoice {

    public enum Status {
        ISSUED,
        PAID,
        VOID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 40, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PaymentTransaction transaction;

    // Set only if a real provider supplies one (e.g. Stripe-hosted invoice PDF); never fabricated.
    @Column(name = "provider_invoice_id", length = 120)
    private String providerInvoiceId;

    @Column(name = "provider_invoice_url", length = 500)
    private String providerInvoiceUrl;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

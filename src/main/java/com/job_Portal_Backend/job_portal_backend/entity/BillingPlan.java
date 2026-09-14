package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A row in the plan catalog. Prices are stored in minor currency units (e.g. paise for INR) and
 * are the ONLY source of truth for what a plan costs — the frontend may display a plan's price
 * but must never submit it back as the authoritative amount for a purchase.
 *
 * <p>Seeded prices (see BillingPlanSeeder) are placeholder example figures, not a real pricing
 * decision — no pricing was specified in the product requirements this repository was built
 * from, and the task explicitly warned against inventing production prices. They are ordinary
 * configuration data, editable without a code change.
 */
@Entity
@Table(name = "billing_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_billing_plan_code", columnNames = "code")
})
@Getter
@Setter
@ToString
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "monthly_price_minor", nullable = false)
    private Long monthlyPriceMinor;

    @Column(name = "annual_price_minor")
    private Long annualPriceMinor;

    // null means unlimited.
    @Column(name = "max_active_jobs")
    private Integer maxActiveJobs;

    // Credits granted each billing cycle a subscription on this plan renews/activates.
    @Column(name = "included_credits", nullable = false)
    private Integer includedCredits = 0;

    // Stored for catalog completeness (task-specified plan property); no feature currently
    // consumes it, since no "featured job" capability exists in this product yet.
    @Column(name = "featured_job_allowance")
    private Integer featuredJobAllowance;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

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

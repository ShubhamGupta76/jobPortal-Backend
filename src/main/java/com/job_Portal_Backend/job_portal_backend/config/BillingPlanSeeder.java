package com.job_Portal_Backend.job_portal_backend.config;

import com.job_Portal_Backend.job_portal_backend.entity.BillingPlan;
import com.job_Portal_Backend.job_portal_backend.repository.BillingPlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the plan catalog if empty. The prices/limits below are clearly-labeled PLACEHOLDER
 * example figures, not a real pricing decision — no pricing was defined anywhere in this
 * repository's product requirements (info(2).md / info(3).md), and the task that introduced
 * billing explicitly warned against inventing production prices. They exist only so the catalog
 * (and everything that depends on it — checkout, entitlements) is non-empty and testable; change
 * them directly in the database (or via a future admin plan-management screen) whenever real
 * pricing is decided. FREE's unlimited job posting matches today's actual behavior exactly, so
 * seeding it changes nothing for any existing company.
 */
@Component
public class BillingPlanSeeder implements CommandLineRunner {

    private final BillingPlanRepository billingPlanRepository;

    public BillingPlanSeeder(BillingPlanRepository billingPlanRepository) {
        this.billingPlanRepository = billingPlanRepository;
    }

    @Override
    public void run(String... args) {
        seed("FREE", "Free", 0L, 0L, null, 0, 0, 0);
        seed("STARTER", "Starter", 999900L, 9999000L, 10, 50, 2, 1);
        seed("PRO", "Pro", 2999900L, 29999000L, 50, 200, 10, 2);
        seed("BUSINESS", "Business", 5999900L, 59999000L, null, 1000, 50, 3);
    }

    private void seed(String code, String displayName, long monthlyPriceMinor, long annualPriceMinor,
            Integer maxActiveJobs, int includedCredits, int featuredJobAllowance, int sortOrder) {
        if (billingPlanRepository.findByCode(code).isPresent()) {
            return;
        }
        BillingPlan plan = new BillingPlan();
        plan.setCode(code);
        plan.setDisplayName(displayName);
        plan.setCurrency("INR");
        plan.setMonthlyPriceMinor(monthlyPriceMinor);
        plan.setAnnualPriceMinor(annualPriceMinor);
        plan.setMaxActiveJobs(maxActiveJobs);
        plan.setIncludedCredits(includedCredits);
        plan.setFeaturedJobAllowance(featuredJobAllowance);
        plan.setActive(true);
        plan.setSortOrder(sortOrder);
        billingPlanRepository.save(plan);
    }
}

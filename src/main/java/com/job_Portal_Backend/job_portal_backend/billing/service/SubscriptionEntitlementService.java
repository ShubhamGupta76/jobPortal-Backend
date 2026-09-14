package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.BillingPlan;
import com.job_Portal_Backend.job_portal_backend.entity.CompanySubscription;
import com.job_Portal_Backend.job_portal_backend.entity.CompanySubscription.Status;
import com.job_Portal_Backend.job_portal_backend.repository.BillingPlanRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanySubscriptionRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * The single place plan entitlements are decided. Callers (JobService, controllers) ask this
 * service a yes/no question; nobody else reads BillingPlan/CompanySubscription rows directly to
 * make an authorization or limit decision, and nobody trusts a plan code the client sends.
 *
 * <p>A company with no CompanySubscription row, or whose subscription has lapsed
 * (CANCELED/EXPIRED), is treated as being on the FREE plan — lazily, with no backfill needed.
 * The seeded FREE plan has no job limit, so this is a strict no-op for every company that existed
 * before this slice: nothing that worked yesterday can start failing today.
 */
@Service
public class SubscriptionEntitlementService {

    private static final String FREE_PLAN_CODE = "FREE";
    private static final Set<Status> ENTITLED_STATUSES = Set.of(Status.TRIALING, Status.ACTIVE, Status.PAST_DUE, Status.PAUSED);

    private final CompanySubscriptionRepository subscriptionRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final JobRepository jobRepository;

    public SubscriptionEntitlementService(CompanySubscriptionRepository subscriptionRepository,
            BillingPlanRepository billingPlanRepository, JobRepository jobRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.billingPlanRepository = billingPlanRepository;
        this.jobRepository = jobRepository;
    }

    public BillingPlan resolveEffectivePlan(Long companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .filter(subscription -> ENTITLED_STATUSES.contains(subscription.getStatus()))
                .map(CompanySubscription::getPlan)
                .orElseGet(this::freePlan);
    }

    public boolean canPostJob(Long companyId) {
        BillingPlan plan = resolveEffectivePlan(companyId);
        if (plan.getMaxActiveJobs() == null) {
            return true;
        }
        return jobRepository.countActiveByCompanyId(companyId) < plan.getMaxActiveJobs();
    }

    public Integer maxActiveJobs(Long companyId) {
        return resolveEffectivePlan(companyId).getMaxActiveJobs();
    }

    public int monthlyCredits(Long companyId) {
        Integer included = resolveEffectivePlan(companyId).getIncludedCredits();
        return included != null ? included : 0;
    }

    private BillingPlan freePlan() {
        return billingPlanRepository.findByCode(FREE_PLAN_CODE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Billing plan catalog is not seeded"));
    }
}

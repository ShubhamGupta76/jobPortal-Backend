package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.BillingPlan;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CompanySubscription;
import com.job_Portal_Backend.job_portal_backend.entity.CompanySubscription.Status;
import com.job_Portal_Backend.job_portal_backend.repository.BillingPlanRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanySubscriptionRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionEntitlementServiceTest {

    private final CompanySubscriptionRepository subscriptionRepository = mock(CompanySubscriptionRepository.class);
    private final BillingPlanRepository billingPlanRepository = mock(BillingPlanRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final SubscriptionEntitlementService service =
            new SubscriptionEntitlementService(subscriptionRepository, billingPlanRepository, jobRepository);

    private BillingPlan plan(String code, Integer maxActiveJobs, int credits) {
        BillingPlan plan = new BillingPlan();
        plan.setCode(code);
        plan.setMaxActiveJobs(maxActiveJobs);
        plan.setIncludedCredits(credits);
        return plan;
    }

    @Test
    void companyWithNoSubscriptionRowIsTreatedAsFreePlan() {
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(billingPlanRepository.findByCode("FREE")).thenReturn(Optional.of(plan("FREE", null, 0)));

        assertEquals("FREE", service.resolveEffectivePlan(1L).getCode());
    }

    @Test
    void freePlanWithNoJobLimitAlwaysAllowsPosting() {
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(billingPlanRepository.findByCode("FREE")).thenReturn(Optional.of(plan("FREE", null, 0)));

        assertTrue(service.canPostJob(1L));
        verifyNoInteractions(jobRepository);
    }

    @Test
    void planWithAJobLimitBlocksPostingOnceReached() {
        Company company = new Company();
        company.setId(1L);
        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setStatus(Status.ACTIVE);
        subscription.setPlan(plan("STARTER", 5, 50));
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(jobRepository.countActiveByCompanyId(1L)).thenReturn(5L);

        assertFalse(service.canPostJob(1L));
    }

    @Test
    void planWithAJobLimitAllowsPostingBelowTheLimit() {
        Company company = new Company();
        company.setId(1L);
        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setStatus(Status.ACTIVE);
        subscription.setPlan(plan("STARTER", 5, 50));
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(jobRepository.countActiveByCompanyId(1L)).thenReturn(4L);

        assertTrue(service.canPostJob(1L));
    }

    @Test
    void canceledSubscriptionRevertsToFreePlanEntitlements() {
        Company company = new Company();
        company.setId(1L);
        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setStatus(Status.CANCELED);
        subscription.setPlan(plan("PRO", 50, 200));
        when(subscriptionRepository.findByCompanyId(1L)).thenReturn(Optional.of(subscription));
        when(billingPlanRepository.findByCode("FREE")).thenReturn(Optional.of(plan("FREE", null, 0)));

        assertEquals("FREE", service.resolveEffectivePlan(1L).getCode());
    }
}

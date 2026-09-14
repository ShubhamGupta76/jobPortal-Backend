package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.billing.dto.CheckoutRequest;
import com.job_Portal_Backend.job_portal_backend.billing.dto.CheckoutResponseDto;
import com.job_Portal_Backend.job_portal_backend.billing.provider.ManualPaymentProvider;
import com.job_Portal_Backend.job_portal_backend.billing.provider.PaymentProvider;
import com.job_Portal_Backend.job_portal_backend.entity.*;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember.TeamRole;
import com.job_Portal_Backend.job_portal_backend.repository.*;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyTeamMemberRepository teamMemberRepository = mock(CompanyTeamMemberRepository.class);
    private final BillingPlanRepository billingPlanRepository = mock(BillingPlanRepository.class);
    private final CompanySubscriptionRepository subscriptionRepository = mock(CompanySubscriptionRepository.class);
    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final CreditLedgerEntryRepository ledgerRepository = mock(CreditLedgerEntryRepository.class);
    private final CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
    private final SubscriptionEntitlementService entitlementService = mock(SubscriptionEntitlementService.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final ManualPaymentProvider paymentProvider = mock(ManualPaymentProvider.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final BillingService service = new BillingService(companyRepository, teamMemberRepository,
            billingPlanRepository, subscriptionRepository, transactionRepository, invoiceRepository, ledgerRepository,
            creditLedgerService, entitlementService, jobRepository, paymentProvider, auditLogService, notificationService);

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private Company company(Long id, User owner) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Inc");
        company.setOwner(owner);
        return company;
    }

    private CompanyTeamMember member(Company company, User user, TeamRole role) {
        CompanyTeamMember member = new CompanyTeamMember();
        member.setCompany(company);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    @Test
    void ownerCanCheckout() {
        User owner = user(1L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));

        BillingPlan plan = new BillingPlan();
        plan.setCode("STARTER");
        plan.setActive(true);
        plan.setCurrency("INR");
        plan.setMonthlyPriceMinor(999900L);
        when(billingPlanRepository.findByCode("STARTER")).thenReturn(Optional.of(plan));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            PaymentTransaction t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        when(paymentProvider.createCheckoutSession(any(), any(), any()))
                .thenReturn(new PaymentProvider.CheckoutResult(null, true, "pending"));

        CheckoutRequest request = new CheckoutRequest();
        request.setPlanCode("STARTER");
        CheckoutResponseDto response = service.checkout(10L, request, owner);

        assertEquals(100L, response.getTransactionId());
        assertTrue(response.isRequiresManualConfirmation());
        verify(transactionRepository).save(argThat(t -> t.getAmountMinor() == 999900L));
    }

    @Test
    void recruiterCannotCheckout() {
        User owner = user(1L);
        User recruiter = user(2L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(member(company, recruiter, TeamRole.RECRUITER)));

        CheckoutRequest request = new CheckoutRequest();
        request.setPlanCode("STARTER");

        assertThrows(ResponseStatusException.class, () -> service.checkout(10L, request, recruiter));
    }

    @Test
    void adminCanViewButCannotCheckout() {
        User owner = user(1L);
        User admin = user(2L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(member(company, admin, TeamRole.ADMIN)));

        CheckoutRequest request = new CheckoutRequest();
        request.setPlanCode("STARTER");
        assertThrows(ResponseStatusException.class, () -> service.checkout(10L, request, admin));

        // but can view the summary
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.empty());
        BillingPlan freePlan = new BillingPlan();
        freePlan.setCode("FREE");
        freePlan.setDisplayName("Free");
        when(entitlementService.resolveEffectivePlan(10L)).thenReturn(freePlan);
        when(creditLedgerService.currentBalance(10L)).thenReturn(0);
        when(jobRepository.countActiveByCompanyId(10L)).thenReturn(0L);

        assertDoesNotThrow(() -> service.getSummary(10L, admin));
    }

    @Test
    void nonMemberCannotAccessBilling() {
        User owner = user(1L);
        User stranger = user(3L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 3L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getSummary(10L, stranger));
    }

    @Test
    void checkoutRejectsUnknownPlanCode() {
        User owner = user(1L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));
        when(billingPlanRepository.findByCode("BOGUS")).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest();
        request.setPlanCode("BOGUS");

        assertThrows(ResponseStatusException.class, () -> service.checkout(10L, request, owner));
    }

    @Test
    void cancelSubscriptionSetsCancelAtPeriodEndRatherThanDestroyingIt() {
        User owner = user(1L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));

        CompanySubscription subscription = new CompanySubscription();
        subscription.setId(5L);
        subscription.setCompany(company);
        subscription.setStatus(CompanySubscription.Status.ACTIVE);
        subscription.setCurrentPeriodEnd(java.time.LocalDateTime.now().plusDays(10));
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.of(subscription));
        when(teamMemberRepository.findByCompanyIdAndRole(10L, TeamRole.OWNER)).thenReturn(java.util.List.of(member(company, owner, TeamRole.OWNER)));

        service.cancelSubscription(10L, owner);

        assertTrue(subscription.getCancelAtPeriodEnd());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelSubscriptionFailsWhenThereIsNoActiveSubscription() {
        User owner = user(1L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.cancelSubscription(10L, owner));
    }

    @Test
    void resumeRestoresAnActiveSubscription() {
        User owner = user(1L);
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));

        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setCancelAtPeriodEnd(true);
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.of(subscription));

        service.resumeSubscription(10L, owner);

        assertFalse(subscription.getCancelAtPeriodEnd());
    }

    @Test
    void applySuccessfulPaymentIsIdempotent() {
        Company company = company(10L, user(1L));
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(200L);
        transaction.setCompany(company);
        transaction.setStatus(PaymentTransaction.Status.SUCCEEDED);
        when(transactionRepository.findById(200L)).thenReturn(Optional.of(transaction));

        service.applySuccessfulPayment(200L);

        verify(companyRepository, never()).findByIdForUpdate(anyLong());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void applySuccessfulPaymentActivatesSubscriptionGrantsCreditsAndIssuesInvoiceExactlyOnce() {
        Company company = company(10L, user(1L));
        BillingPlan plan = new BillingPlan();
        plan.setCode("STARTER");
        plan.setDisplayName("Starter");
        plan.setIncludedCredits(50);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(200L);
        transaction.setCompany(company);
        transaction.setPlan(plan);
        transaction.setPurpose(PaymentTransaction.Purpose.SUBSCRIPTION);
        transaction.setBillingCycle(PaymentTransaction.BillingCycle.MONTHLY);
        transaction.setProvider("MANUAL");
        transaction.setAmountMinor(999900L);
        transaction.setCurrency("INR");
        transaction.setStatus(PaymentTransaction.Status.PENDING);

        when(transactionRepository.findById(200L)).thenReturn(Optional.of(transaction));
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company));
        when(subscriptionRepository.findByCompanyId(10L)).thenReturn(Optional.empty());
        when(invoiceRepository.findByTransactionId(200L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findByCompanyIdAndRole(10L, TeamRole.OWNER)).thenReturn(java.util.List.of());

        service.applySuccessfulPayment(200L);

        assertEquals(PaymentTransaction.Status.SUCCEEDED, transaction.getStatus());
        verify(subscriptionRepository).save(argThat(s -> s.getStatus() == CompanySubscription.Status.ACTIVE));
        verify(creditLedgerService).grant(eq(10L), eq(50), eq(CreditLedgerEntry.Type.SUBSCRIPTION_GRANT),
                eq("PaymentTransaction"), eq(200L), anyString(), isNull());
        verify(invoiceRepository).save(argThat(i -> i.getStatus() == Invoice.Status.PAID));
    }
}

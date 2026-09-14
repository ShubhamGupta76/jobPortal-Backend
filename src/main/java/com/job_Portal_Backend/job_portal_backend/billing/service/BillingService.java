package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.billing.dto.*;
import com.job_Portal_Backend.job_portal_backend.billing.provider.ManualPaymentProvider;
import com.job_Portal_Backend.job_portal_backend.billing.provider.PaymentProvider;
import com.job_Portal_Backend.job_portal_backend.entity.*;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember.TeamRole;
import com.job_Portal_Backend.job_portal_backend.entity.CompanySubscription.Status;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction.BillingCycle;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction.Purpose;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.*;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Company-scoped billing operations: plan catalog, subscription checkout/cancel/resume, and
 * transaction/invoice/credit-activity history. Every method that touches a specific company
 * resolves the actor's team role itself (never trusting a role the client claims) using the same
 * lazy-owner-backfill pattern CompanyTeamService already established, and enforces the
 * OWNER-manages / OWNER+ADMIN-view split from the task's own authorization requirements.
 *
 * <p>All state-changing payment outcomes funnel through {@link #applySuccessfulPayment} /
 * {@link #applyFailedPayment}, called from exactly two places: the webhook handler
 * (BillingWebhookService) and the audited admin confirmation path (BillingAdminService) — never
 * directly from a company-facing endpoint.
 */
@Service
public class BillingService {

    private static final Set<TeamRole> MANAGE_ROLES = Set.of(TeamRole.OWNER);
    private static final Set<TeamRole> VIEW_ROLES = Set.of(TeamRole.OWNER, TeamRole.ADMIN);
    private static final int DEFAULT_LOW_CREDIT_THRESHOLD = 10;

    private final CompanyRepository companyRepository;
    private final CompanyTeamMemberRepository teamMemberRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreditLedgerEntryRepository ledgerRepository;
    private final CreditLedgerService creditLedgerService;
    private final SubscriptionEntitlementService entitlementService;
    private final JobRepository jobRepository;
    private final ManualPaymentProvider paymentProvider;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public BillingService(CompanyRepository companyRepository, CompanyTeamMemberRepository teamMemberRepository,
            BillingPlanRepository billingPlanRepository, CompanySubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository transactionRepository, InvoiceRepository invoiceRepository,
            CreditLedgerEntryRepository ledgerRepository, CreditLedgerService creditLedgerService,
            SubscriptionEntitlementService entitlementService, JobRepository jobRepository,
            ManualPaymentProvider paymentProvider, AuditLogService auditLogService,
            NotificationService notificationService) {
        this.companyRepository = companyRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.billingPlanRepository = billingPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.ledgerRepository = ledgerRepository;
        this.creditLedgerService = creditLedgerService;
        this.entitlementService = entitlementService;
        this.jobRepository = jobRepository;
        this.paymentProvider = paymentProvider;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    // --- read-only, public -------------------------------------------------

    public List<BillingPlanDto> listActivePlans() {
        return billingPlanRepository.findByActiveTrueOrderBySortOrderAsc().stream().map(this::toPlanDto).toList();
    }

    // --- company-scoped reads ------------------------------------------------

    public BillingSummaryDto getSummary(Long companyId, User actor) {
        Company company = requireCompany(companyId);
        TeamRole role = requireActorRole(company, actor, VIEW_ROLES);

        Optional<CompanySubscription> subscription = subscriptionRepository.findByCompanyId(companyId);
        BillingPlan effectivePlan = entitlementService.resolveEffectivePlan(companyId);

        SubscriptionDto subscriptionDto = subscription
                .map(s -> toSubscriptionDto(s.getPlan(), s.getStatus().name(), s.getProvider(),
                        s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), Boolean.TRUE.equals(s.getCancelAtPeriodEnd())))
                .orElseGet(() -> toSubscriptionDto(effectivePlan, "NONE", null, null, null, false));

        return new BillingSummaryDto(
                subscriptionDto,
                creditLedgerService.currentBalance(companyId),
                DEFAULT_LOW_CREDIT_THRESHOLD,
                effectivePlan.getMaxActiveJobs(),
                jobRepository.countActiveByCompanyId(companyId),
                role.name());
    }

    public Page<PaymentTransactionDto> listTransactions(Long companyId, User actor, Pageable pageable) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, VIEW_ROLES);
        return transactionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable).map(this::toTransactionDto);
    }

    public Page<InvoiceDto> listInvoices(Long companyId, User actor, Pageable pageable) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, VIEW_ROLES);
        return invoiceRepository.findByCompanyIdOrderByIssueDateDesc(companyId, pageable).map(this::toInvoiceDto);
    }

    public Page<CreditLedgerEntryDto> listCreditActivity(Long companyId, User actor, Pageable pageable) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, VIEW_ROLES);
        return ledgerRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable).map(this::toLedgerDto);
    }

    // --- mutations -------------------------------------------------------

    @Transactional
    public CheckoutResponseDto checkout(Long companyId, CheckoutRequest request, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<PaymentTransaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent() && existing.get().getCompany().getId().equals(companyId)) {
                return toCheckoutResponse(existing.get(), "Existing checkout for this idempotency key.");
            }
        }

        BillingPlan plan = billingPlanRepository.findByCode(request.getPlanCode())
                .filter(BillingPlan::getActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive plan"));

        BillingCycle cycle = parseCycle(request.getBillingCycle());
        Long amountMinor = cycle == BillingCycle.ANNUAL ? plan.getAnnualPriceMinor() : plan.getMonthlyPriceMinor();
        if (amountMinor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This plan does not support " + cycle + " billing");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCompany(company);
        transaction.setPlan(plan);
        transaction.setPurpose(Purpose.SUBSCRIPTION);
        transaction.setBillingCycle(cycle);
        transaction.setProvider(paymentProvider.getProviderName());
        transaction.setAmountMinor(amountMinor);
        transaction.setCurrency(plan.getCurrency());
        transaction.setStatus(PaymentTransaction.Status.PENDING);
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction = transactionRepository.save(transaction);

        PaymentProvider.CheckoutResult result = paymentProvider.createCheckoutSession(company, plan, transaction);

        auditLogService.logAction("PaymentTransaction", transaction.getId(), "CHECKOUT_CREATED", actor,
                "Checkout created for plan " + plan.getCode() + " (" + cycle + "), amount " + amountMinor
                        + " " + plan.getCurrency() + " minor units", null, null);

        return new CheckoutResponseDto(transaction.getId(), transaction.getStatus().name(), result.checkoutUrl(),
                result.requiresManualConfirmation(), result.message());
    }

    @Transactional
    public void cancelSubscription(Long companyId, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);
        companyRepository.findByIdForUpdate(companyId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .filter(s -> s.getStatus() == Status.ACTIVE || s.getStatus() == Status.TRIALING || s.getStatus() == Status.PAST_DUE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active subscription to cancel"));

        subscription.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(subscription);

        auditLogService.logAction("CompanySubscription", subscription.getId(), "CANCEL_AT_PERIOD_END", actor,
                "Subscription for " + company.getName() + " set to cancel at period end", null, null);
        notifyOwners(company, "Subscription canceled",
                "Your subscription will remain active until " + subscription.getCurrentPeriodEnd() + " and will not renew.",
                "SUBSCRIPTION_CANCELED");
    }

    @Transactional
    public void resumeSubscription(Long companyId, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);
        companyRepository.findByIdForUpdate(companyId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .filter(s -> Boolean.TRUE.equals(s.getCancelAtPeriodEnd()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No pending cancellation to resume"));

        subscription.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(subscription);

        auditLogService.logAction("CompanySubscription", subscription.getId(), "RESUME", actor,
                "Subscription cancellation resumed for " + company.getName(), null, null);
    }

    // --- shared, provider-authoritative payment outcome handling -----------

    /** Idempotent: calling this twice for an already-SUCCEEDED transaction is a safe no-op. */
    @Transactional
    public void applySuccessfulPayment(Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (transaction.getStatus() == PaymentTransaction.Status.SUCCEEDED) {
            return;
        }

        Long companyId = transaction.getCompany().getId();
        companyRepository.findByIdForUpdate(companyId);

        transaction.setStatus(PaymentTransaction.Status.SUCCEEDED);
        transactionRepository.save(transaction);

        if (transaction.getPurpose() == Purpose.SUBSCRIPTION && transaction.getPlan() != null) {
            activateSubscription(transaction);
        }

        if (transaction.getPlan() != null && transaction.getPlan().getIncludedCredits() != null
                && transaction.getPlan().getIncludedCredits() > 0) {
            creditLedgerService.grant(companyId, transaction.getPlan().getIncludedCredits(), Type.SUBSCRIPTION_GRANT,
                    "PaymentTransaction", transaction.getId(),
                    "Credits granted for " + transaction.getPlan().getCode() + " subscription", null);
        }

        issueInvoice(transaction);

        auditLogService.logAction("PaymentTransaction", transaction.getId(), "PAYMENT_SUCCEEDED", null,
                "Payment succeeded for " + transaction.getCompany().getName(), null, null);
        notifyOwners(transaction.getCompany(), "Payment received",
                "Your payment for " + (transaction.getPlan() != null ? transaction.getPlan().getDisplayName() : "your order")
                        + " was confirmed.",
                "PAYMENT_SUCCEEDED");
    }

    @Transactional
    public void applyFailedPayment(Long transactionId) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (transaction.getStatus() != PaymentTransaction.Status.PENDING) {
            return;
        }
        transaction.setStatus(PaymentTransaction.Status.FAILED);
        transactionRepository.save(transaction);

        auditLogService.logAction("PaymentTransaction", transaction.getId(), "PAYMENT_FAILED", null,
                "Payment failed for " + transaction.getCompany().getName(), null, null);
        notifyOwners(transaction.getCompany(), "Payment failed",
                "Your recent payment could not be completed. Please try again.", "PAYMENT_FAILED");
    }

    private void activateSubscription(PaymentTransaction transaction) {
        Company company = transaction.getCompany();
        CompanySubscription subscription = subscriptionRepository.findByCompanyId(company.getId())
                .orElseGet(() -> {
                    CompanySubscription created = new CompanySubscription();
                    created.setCompany(company);
                    return created;
                });

        LocalDateTime now = LocalDateTime.now();
        subscription.setPlan(transaction.getPlan());
        subscription.setStatus(Status.ACTIVE);
        subscription.setProvider(transaction.getProvider());
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(transaction.getBillingCycle() == BillingCycle.ANNUAL
                ? now.plusYears(1) : now.plusDays(30));
        subscription.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(subscription);

        notifyOwners(company, "Subscription activated",
                "Your " + transaction.getPlan().getDisplayName() + " subscription is now active.",
                "SUBSCRIPTION_ACTIVATED");
    }

    private void issueInvoice(PaymentTransaction transaction) {
        if (invoiceRepository.findByTransactionId(transaction.getId()).isPresent()) {
            return;
        }
        Invoice invoice = new Invoice();
        invoice.setCompany(transaction.getCompany());
        invoice.setTransaction(transaction);
        invoice.setInvoiceNumber("INV-" + String.format("%08d", transaction.getId()));
        invoice.setAmountMinor(transaction.getAmountMinor());
        invoice.setCurrency(transaction.getCurrency());
        invoice.setStatus(Invoice.Status.PAID);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setPaidDate(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    // --- authorization helpers (mirrors CompanyTeamService's pattern) ------

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    private TeamRole resolveActorRole(Company company, User actor) {
        if (company.getOwner() != null && teamMemberRepository.countByCompanyId(company.getId()) == 0) {
            CompanyTeamMember ownerMembership = new CompanyTeamMember();
            ownerMembership.setCompany(company);
            ownerMembership.setUser(company.getOwner());
            ownerMembership.setRole(TeamRole.OWNER);
            teamMemberRepository.save(ownerMembership);
        }
        return teamMemberRepository.findByCompanyIdAndUserId(company.getId(), actor.getId())
                .map(CompanyTeamMember::getRole)
                .orElse(company.getOwner() != null && company.getOwner().getId().equals(actor.getId()) ? TeamRole.OWNER : null);
    }

    private TeamRole requireActorRole(Company company, User actor, Set<TeamRole> allowed) {
        TeamRole role = resolveActorRole(company, actor);
        if (role == null || !allowed.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access billing for this company.");
        }
        return role;
    }

    private void notifyOwners(Company company, String title, String message, String type) {
        List<CompanyTeamMember> owners = teamMemberRepository.findByCompanyIdAndRole(company.getId(), TeamRole.OWNER);
        for (CompanyTeamMember owner : owners) {
            notificationService.sendNotificationToUser(owner.getUser(), title, message, type);
        }
        if (owners.isEmpty() && company.getOwner() != null) {
            notificationService.sendNotificationToUser(company.getOwner(), title, message, type);
        }
    }

    private BillingCycle parseCycle(String value) {
        if (value == null || value.isBlank()) {
            return BillingCycle.MONTHLY;
        }
        try {
            return BillingCycle.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid billing cycle: " + value);
        }
    }

    // --- mapping -----------------------------------------------------------

    private BillingPlanDto toPlanDto(BillingPlan plan) {
        return new BillingPlanDto(plan.getCode(), plan.getDisplayName(), plan.getCurrency(),
                plan.getMonthlyPriceMinor(), plan.getAnnualPriceMinor(), plan.getMaxActiveJobs(),
                plan.getIncludedCredits(), plan.getFeaturedJobAllowance());
    }

    private SubscriptionDto toSubscriptionDto(BillingPlan plan, String status, String provider,
            LocalDateTime periodStart, LocalDateTime periodEnd, boolean cancelAtPeriodEnd) {
        return new SubscriptionDto(plan.getCode(), plan.getDisplayName(), status, provider,
                periodStart, periodEnd, cancelAtPeriodEnd);
    }

    private PaymentTransactionDto toTransactionDto(PaymentTransaction transaction) {
        return new PaymentTransactionDto(transaction.getId(), transaction.getPurpose().name(),
                transaction.getPlan() != null ? transaction.getPlan().getCode() : null,
                transaction.getProvider(), transaction.getAmountMinor(), transaction.getCurrency(),
                transaction.getStatus().name(), transaction.getCreatedAt());
    }

    private InvoiceDto toInvoiceDto(Invoice invoice) {
        return new InvoiceDto(invoice.getId(), invoice.getInvoiceNumber(), invoice.getAmountMinor(),
                invoice.getCurrency(), invoice.getStatus().name(), invoice.getIssueDate(), invoice.getPaidDate(),
                invoice.getProviderInvoiceUrl());
    }

    private CreditLedgerEntryDto toLedgerDto(CreditLedgerEntry entry) {
        return new CreditLedgerEntryDto(entry.getId(), entry.getAmount(), entry.getType().name(),
                entry.getDescription(), entry.getBalanceAfter(), entry.getCreatedAt());
    }

    private CheckoutResponseDto toCheckoutResponse(PaymentTransaction transaction, String message) {
        return new CheckoutResponseDto(transaction.getId(), transaction.getStatus().name(), null, true, message);
    }
}

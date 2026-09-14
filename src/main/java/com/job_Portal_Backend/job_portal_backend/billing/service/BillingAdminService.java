package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.billing.dto.AdminTransactionDto;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.PaymentTransactionRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Privileged, platform-admin-only billing operations — never reachable by a company's own users.
 * Every method here requires an explicit reason and writes an AuditLog entry; this is the ONLY
 * non-webhook path by which a transaction can move to SUCCEEDED, and the only path that can move
 * a company's credit balance without an underlying payment.
 */
@Service
public class BillingAdminService {

    private final PaymentTransactionRepository transactionRepository;
    private final BillingService billingService;
    private final CreditLedgerService creditLedgerService;
    private final AuditLogService auditLogService;

    public BillingAdminService(PaymentTransactionRepository transactionRepository, BillingService billingService,
            CreditLedgerService creditLedgerService, AuditLogService auditLogService) {
        this.transactionRepository = transactionRepository;
        this.billingService = billingService;
        this.creditLedgerService = creditLedgerService;
        this.auditLogService = auditLogService;
    }

    /**
     * Manual reconciliation for the no-real-gateway environment (see ManualPaymentProvider): an
     * admin confirms a payment was received out-of-band (bank transfer, etc.). Only PENDING
     * transactions may be confirmed, and this funnels through the exact same
     * {@code applySuccessfulPayment} used by the webhook path, so activation/credit-grant/invoice
     * logic never diverges between the two.
     */
    @Transactional
    public void confirmPayment(Long transactionId, String reason, User admin) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (transaction.getStatus() != PaymentTransaction.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending transactions can be confirmed");
        }

        billingService.applySuccessfulPayment(transactionId);

        auditLogService.logAction("PaymentTransaction", transactionId, "ADMIN_CONFIRM_PAYMENT", admin,
                "Admin manually confirmed payment. Reason: " + reason, null, null);
    }

    @Transactional
    public int adjustCredits(Long companyId, int amount, String reason, User admin) {
        if (amount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment amount must not be zero");
        }

        int newBalance = amount > 0
                ? creditLedgerService.grant(companyId, amount, Type.ADMIN_ADJUSTMENT, null, null, reason, admin)
                : creditLedgerService.consume(companyId, -amount, Type.ADMIN_ADJUSTMENT, null, null, reason, admin);

        auditLogService.logAction("Company", companyId, "ADMIN_CREDIT_ADJUSTMENT", admin,
                "Admin adjusted credits by " + amount + ". Reason: " + reason, null, null);
        return newBalance;
    }

    public Page<AdminTransactionDto> listAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(transaction -> new AdminTransactionDto(
                transaction.getId(),
                transaction.getCompany().getId(),
                transaction.getCompany().getName(),
                transaction.getPurpose().name(),
                transaction.getPlan() != null ? transaction.getPlan().getCode() : null,
                transaction.getAmountMinor(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()));
    }
}

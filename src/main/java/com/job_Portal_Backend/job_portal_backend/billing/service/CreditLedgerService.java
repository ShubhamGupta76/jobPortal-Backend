package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CreditLedgerEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The only place a company's credit balance is ever changed. Every mutation appends an immutable
 * ledger row inside a transaction that holds a PESSIMISTIC_WRITE lock on the company row for its
 * duration (CompanyRepository.findByIdForUpdate), so concurrent grants/consumptions for the same
 * company are strictly serialized — no lost updates, no negative balances, no double consumption.
 *
 * <p>Balance is derived from the ledger (the latest entry's balanceAfter, itself the running sum),
 * never a separate mutable counter column, so "company.credits = N" style state can't drift from
 * its history.
 */
@Service
public class CreditLedgerService {

    private final CompanyRepository companyRepository;
    private final CreditLedgerEntryRepository ledgerRepository;

    public CreditLedgerService(CompanyRepository companyRepository, CreditLedgerEntryRepository ledgerRepository) {
        this.companyRepository = companyRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public int currentBalance(Long companyId) {
        return ledgerRepository.findLatestByCompanyId(companyId).map(CreditLedgerEntry::getBalanceAfter).orElse(0);
    }

    /**
     * Grants credits. If referenceType/referenceId are given and an entry already exists for that
     * exact reference, the grant is skipped and the current balance returned unchanged — this is
     * what makes granting credits for the same successful payment/webhook event idempotent under
     * retries or duplicate delivery.
     */
    @Transactional
    public int grant(Long companyId, int amount, Type type, String referenceType, Long referenceId,
            String description, User actor) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Grant amount must be positive");
        }
        Company company = lockCompany(companyId);

        if (referenceType != null && referenceId != null
                && ledgerRepository.existsByReferenceTypeAndReferenceId(referenceType, referenceId)) {
            return currentBalance(companyId);
        }

        int newBalance = currentBalance(companyId) + amount;
        appendEntry(company, amount, type, referenceType, referenceId, description, newBalance, actor);
        return newBalance;
    }

    /**
     * Consumes credits atomically. Throws 402 PAYMENT_REQUIRED (never silently clamping to zero)
     * if the balance is insufficient, so the caller can abort whatever billable action triggered
     * this consumption without having partially applied it.
     */
    @Transactional
    public int consume(Long companyId, int amount, Type type, String referenceType, Long referenceId,
            String description, User actor) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Consume amount must be positive");
        }
        Company company = lockCompany(companyId);

        if (referenceType != null && referenceId != null
                && ledgerRepository.existsByReferenceTypeAndReferenceId(referenceType, referenceId)) {
            return currentBalance(companyId);
        }

        int balance = currentBalance(companyId);
        if (balance < amount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits");
        }

        int newBalance = balance - amount;
        appendEntry(company, -amount, type, referenceType, referenceId, description, newBalance, actor);
        return newBalance;
    }

    private Company lockCompany(Long companyId) {
        return companyRepository.findByIdForUpdate(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private void appendEntry(Company company, int signedAmount, Type type, String referenceType, Long referenceId,
            String description, int balanceAfter, User actor) {
        CreditLedgerEntry entry = new CreditLedgerEntry();
        entry.setCompany(company);
        entry.setAmount(signedAmount);
        entry.setType(type);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setDescription(description);
        entry.setBalanceAfter(balanceAfter);
        entry.setCreatedBy(actor);
        ledgerRepository.save(entry);
    }
}

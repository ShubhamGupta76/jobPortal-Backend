package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry;
import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CreditLedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreditLedgerServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CreditLedgerEntryRepository ledgerRepository = mock(CreditLedgerEntryRepository.class);
    private final CreditLedgerService service = new CreditLedgerService(companyRepository, ledgerRepository);

    private Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Inc");
        return company;
    }

    private CreditLedgerEntry entryWithBalance(int balance) {
        CreditLedgerEntry entry = new CreditLedgerEntry();
        entry.setBalanceAfter(balance);
        return entry;
    }

    @Test
    void grantAppendsAPositiveEntryAndReturnsNewBalance() {
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company(10L)));
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.of(entryWithBalance(50)));

        int newBalance = service.grant(10L, 20, Type.SUBSCRIPTION_GRANT, "PaymentTransaction", 5L, "grant", null);

        assertEquals(70, newBalance);
        verify(ledgerRepository).save(argThat(e -> e.getAmount() == 20 && e.getBalanceAfter() == 70));
    }

    @Test
    void grantIsIdempotentForTheSameReference() {
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company(10L)));
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.of(entryWithBalance(50)));
        when(ledgerRepository.existsByReferenceTypeAndReferenceId("PaymentTransaction", 5L)).thenReturn(true);

        int balance = service.grant(10L, 20, Type.SUBSCRIPTION_GRANT, "PaymentTransaction", 5L, "grant", null);

        assertEquals(50, balance);
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeReducesBalanceWhenSufficient() {
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company(10L)));
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.of(entryWithBalance(50)));

        int newBalance = service.consume(10L, 30, Type.JOB_POST, null, null, "consume", null);

        assertEquals(20, newBalance);
        verify(ledgerRepository).save(argThat(e -> e.getAmount() == -30 && e.getBalanceAfter() == 20));
    }

    @Test
    void consumeRejectsInsufficientBalanceWithoutMutatingAnything() {
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company(10L)));
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.of(entryWithBalance(10)));

        assertThrows(ResponseStatusException.class,
                () -> service.consume(10L, 30, Type.JOB_POST, null, null, "consume", null));
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void consumeNeverProducesANegativeBalance() {
        when(companyRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(company(10L)));
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.consume(10L, 1, Type.JOB_POST, null, null, "consume", null));
    }

    @Test
    void balanceWithNoLedgerHistoryIsZero() {
        when(ledgerRepository.findLatestByCompanyId(10L)).thenReturn(Optional.empty());
        assertEquals(0, service.currentBalance(10L));
    }
}

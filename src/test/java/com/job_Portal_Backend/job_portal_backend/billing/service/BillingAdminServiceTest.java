package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry.Type;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.PaymentTransactionRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingAdminServiceTest {

    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final BillingService billingService = mock(BillingService.class);
    private final CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final BillingAdminService service =
            new BillingAdminService(transactionRepository, billingService, creditLedgerService, auditLogService);

    private User admin(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("admin@example.com");
        return user;
    }

    @Test
    void confirmPaymentOnlyAllowsPendingTransactions() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(1L);
        transaction.setStatus(PaymentTransaction.Status.SUCCEEDED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThrows(ResponseStatusException.class, () -> service.confirmPayment(1L, "bank transfer received", admin(9L)));
        verify(billingService, never()).applySuccessfulPayment(anyLong());
    }

    @Test
    void confirmPaymentDelegatesToTheSameFlowAsWebhooksAndAudits() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(1L);
        transaction.setStatus(PaymentTransaction.Status.PENDING);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        service.confirmPayment(1L, "bank transfer received", admin(9L));

        verify(billingService).applySuccessfulPayment(1L);
        verify(auditLogService).logAction(eq("PaymentTransaction"), eq(1L), eq("ADMIN_CONFIRM_PAYMENT"), any(),
                contains("bank transfer received"), isNull(), isNull());
    }

    @Test
    void positiveAdjustmentGrantsCredits() {
        when(creditLedgerService.grant(eq(10L), eq(25), eq(Type.ADMIN_ADJUSTMENT), isNull(), isNull(), anyString(), any()))
                .thenReturn(75);

        int balance = service.adjustCredits(10L, 25, "goodwill credit", admin(9L));

        assertEquals(75, balance);
        verify(creditLedgerService, never()).consume(anyLong(), anyInt(), any(), any(), any(), anyString(), any());
    }

    @Test
    void negativeAdjustmentConsumesCreditsAndCannotGoNegative() {
        when(creditLedgerService.consume(eq(10L), eq(25), eq(Type.ADMIN_ADJUSTMENT), isNull(), isNull(), anyString(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "Insufficient credits"));

        assertThrows(ResponseStatusException.class, () -> service.adjustCredits(10L, -25, "correction", admin(9L)));
    }

    @Test
    void zeroAdjustmentIsRejected() {
        assertThrows(ResponseStatusException.class, () -> service.adjustCredits(10L, 0, "no-op", admin(9L)));
        verifyNoInteractions(creditLedgerService);
    }
}

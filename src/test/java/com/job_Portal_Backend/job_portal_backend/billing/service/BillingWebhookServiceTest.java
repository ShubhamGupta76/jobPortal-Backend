package com.job_Portal_Backend.job_portal_backend.billing.service;

import com.job_Portal_Backend.job_portal_backend.billing.provider.ManualPaymentProvider;
import com.job_Portal_Backend.job_portal_backend.billing.provider.PaymentProvider;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import com.job_Portal_Backend.job_portal_backend.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingWebhookServiceTest {

    private final ManualPaymentProvider paymentProvider = mock(ManualPaymentProvider.class);
    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final BillingService billingService = mock(BillingService.class);
    private final WebhookEventRecorder webhookEventRecorder = mock(WebhookEventRecorder.class);

    private final BillingWebhookService service =
            new BillingWebhookService(paymentProvider, transactionRepository, billingService, webhookEventRecorder);

    @Test
    void validSignatureAndKnownTransactionAppliesSuccessAndMarksProcessed() {
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        when(paymentProvider.verifyWebhook("payload", "sig"))
                .thenReturn(new PaymentProvider.WebhookEvent("evt1", "PAYMENT_SUCCEEDED", "txn-1", "SUCCEEDED"));
        when(webhookEventRecorder.recordIfNew("MANUAL", "evt1", "PAYMENT_SUCCEEDED", "payload")).thenReturn(Optional.of(1L));

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(200L);
        Company company = new Company();
        company.setId(10L);
        transaction.setCompany(company);
        when(transactionRepository.findByProviderAndProviderTransactionId("MANUAL", "txn-1"))
                .thenReturn(Optional.of(transaction));

        service.handle("MANUAL", "payload", "sig");

        verify(billingService).applySuccessfulPayment(200L);
        verify(webhookEventRecorder).markProcessed(1L);
    }

    @Test
    void invalidSignatureIsRejectedBeforeAnyRecordOrProcessing() {
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        when(paymentProvider.verifyWebhook("payload", "bad-sig"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid webhook signature"));

        assertThrows(ResponseStatusException.class, () -> service.handle("MANUAL", "payload", "bad-sig"));
        verifyNoInteractions(webhookEventRecorder);
        verifyNoInteractions(billingService);
    }

    @Test
    void duplicateEventIsIgnoredAndNeverReachesBillingService() {
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        when(paymentProvider.verifyWebhook("payload", "sig"))
                .thenReturn(new PaymentProvider.WebhookEvent("evt1", "PAYMENT_SUCCEEDED", "txn-1", "SUCCEEDED"));
        when(webhookEventRecorder.recordIfNew("MANUAL", "evt1", "PAYMENT_SUCCEEDED", "payload")).thenReturn(Optional.empty());

        service.handle("MANUAL", "payload", "sig");

        verifyNoInteractions(billingService);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void unknownProviderPathIsRejected() {
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        assertThrows(ResponseStatusException.class, () -> service.handle("STRIPE", "payload", "sig"));
        verifyNoInteractions(webhookEventRecorder);
    }

    @Test
    void webhookForAnUnknownTransactionMarksTheEventFailed() {
        when(paymentProvider.getProviderName()).thenReturn("MANUAL");
        when(paymentProvider.verifyWebhook("payload", "sig"))
                .thenReturn(new PaymentProvider.WebhookEvent("evt1", "PAYMENT_SUCCEEDED", "txn-unknown", "SUCCEEDED"));
        when(webhookEventRecorder.recordIfNew("MANUAL", "evt1", "PAYMENT_SUCCEEDED", "payload")).thenReturn(Optional.of(1L));
        when(transactionRepository.findByProviderAndProviderTransactionId("MANUAL", "txn-unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.handle("MANUAL", "payload", "sig"));
        verify(webhookEventRecorder).markFailed(eq(1L), anyString());
        verifyNoInteractions(billingService);
    }
}

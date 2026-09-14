package com.job_Portal_Backend.job_portal_backend.billing.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class ManualPaymentProviderTest {

    private static final String SECRET = "test-webhook-secret";

    private final ManualPaymentProvider provider = new ManualPaymentProvider(new ObjectMapper());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "webhookSecret", SECRET);
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validSignatureAndPayloadAreParsedCorrectly() throws Exception {
        String payload = "{\"eventId\":\"evt1\",\"eventType\":\"PAYMENT_SUCCEEDED\",\"providerTransactionId\":\"txn-1\",\"status\":\"SUCCEEDED\"}";
        String signature = "sha256=" + sign(payload);

        PaymentProvider.WebhookEvent event = provider.verifyWebhook(payload, signature);

        assertEquals("evt1", event.providerEventId());
        assertEquals("txn-1", event.providerTransactionId());
        assertEquals("SUCCEEDED", event.status());
    }

    @Test
    void invalidSignatureIsRejected() {
        String payload = "{\"eventId\":\"evt1\",\"eventType\":\"PAYMENT_SUCCEEDED\",\"providerTransactionId\":\"txn-1\",\"status\":\"SUCCEEDED\"}";
        assertThrows(ResponseStatusException.class, () -> provider.verifyWebhook(payload, "sha256=deadbeef"));
    }

    @Test
    void missingSignatureIsRejected() {
        assertThrows(ResponseStatusException.class, () -> provider.verifyWebhook("{}", null));
    }

    @Test
    void malformedPayloadIsRejectedEvenWithAValidSignature() throws Exception {
        String payload = "not-json";
        String signature = "sha256=" + sign(payload);
        assertThrows(ResponseStatusException.class, () -> provider.verifyWebhook(payload, signature));
    }

    @Test
    void payloadMissingARequiredFieldIsRejected() throws Exception {
        String payload = "{\"eventId\":\"evt1\"}";
        String signature = "sha256=" + sign(payload);
        assertThrows(ResponseStatusException.class, () -> provider.verifyWebhook(payload, signature));
    }

    @Test
    void createCheckoutSessionNeverReportsSuccess() {
        PaymentProvider.CheckoutResult result = provider.createCheckoutSession(null, null, null);
        assertTrue(result.requiresManualConfirmation());
        assertNull(result.checkoutUrl());
    }
}

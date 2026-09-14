package com.job_Portal_Backend.job_portal_backend.billing.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_Portal_Backend.job_portal_backend.entity.BillingPlan;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * The only PaymentProvider implementation active in this repository, because no real gateway
 * (e.g. Stripe) is installed, configured, or has credentials available in this environment — see
 * info(2).md's billing section for the audit that established this. It never reports a payment as
 * successful on its own: creating a checkout session only ever returns
 * {@code requiresManualConfirmation=true}, and the only way a transaction becomes SUCCEEDED is
 * (a) a correctly HMAC-signed webhook call, matching the contract this class verifies, or
 * (b) an explicitly audited admin confirmation (see BillingAdminService) — never a plain client
 * request. Swapping in a real provider means implementing this same interface against that
 * provider's SDK; nothing else in the billing domain needs to change.
 */
@Component
public class ManualPaymentProvider implements PaymentProvider {

    private static final String PROVIDER_NAME = "MANUAL";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;

    @Value("${app.billing.webhook-secret:}")
    private String webhookSecret;

    public ManualPaymentProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public CheckoutResult createCheckoutSession(Company company, BillingPlan plan, PaymentTransaction transaction) {
        return new CheckoutResult(null, true,
                "Online payment is not configured in this environment. An administrator will confirm this "
                        + "payment manually once received; your transaction is recorded as pending.");
    }

    @Override
    public WebhookEvent verifyWebhook(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Webhook processing is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook signature");
        }

        String expected = hmacHex(rawPayload);
        String provided = signatureHeader.startsWith("sha256=") ? signatureHeader.substring(7) : signatureHeader;
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            String eventId = textOrThrow(node, "eventId");
            String eventType = textOrThrow(node, "eventType");
            String providerTransactionId = textOrThrow(node, "providerTransactionId");
            String status = textOrThrow(node, "status");
            return new WebhookEvent(eventId, eventType, providerTransactionId, status);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
        }
    }

    private String textOrThrow(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payload missing field: " + field);
        }
        return value.asText();
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute webhook signature", e);
        }
    }
}

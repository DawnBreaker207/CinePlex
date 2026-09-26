package com.dawn.payment.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// Test-only gateway that confirms any payment. MUST stay disabled in prod
// (app.payment.mock-enabled=false) or anyone can book for free.
@Component
@ConditionalOnProperty(name = "dawn.app.payment.mock-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockPaymentHandler implements PaymentHandler {

    @Value("${service.url.base}")
    private String backendBaseUrl;

    @Override
    public Boolean supports(String provider) {
        return "mock".equalsIgnoreCase(provider);
    }

    @Override
    public String createPaymentUrl(String reservationId, Integer totalAmount, String ipAddress) {
        String base = backendBaseUrl.endsWith("/") ? backendBaseUrl : backendBaseUrl + "/";
        return base + "api/v1/payment/callback/mock"
                + "?reservationId=" + reservationId
                + "&amount=" + totalAmount
                + "&status=SUCCESS";
    }

    @Override
    public Boolean verifySignature(Map<String, String> params) {
        return "SUCCESS".equalsIgnoreCase(params.get("status"));
    }

    @Override
    public String getId(Map<String, String> params) {
        return params.get("reservationId");
    }

    @Override
    public String getTxnRef(Map<String, String> params) {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    public BigDecimal getAmount(Map<String, String> params) {
        String amount = params.get("amount");
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            log.warn("Invalid mock amount '{}'", amount);
            return null;
        }
    }
}

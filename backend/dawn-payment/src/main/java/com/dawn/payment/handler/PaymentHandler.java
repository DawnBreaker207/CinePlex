package com.dawn.payment.handler;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentHandler {
    Boolean supports(String provider);

    String createPaymentUrl(String reservationId, Integer amount, String ipAddress);

    Boolean verifySignature(Map<String, String> params);

    String getId(Map<String, String> params);

    String getTxnRef(Map<String, String> params);

    default BigDecimal getAmount(Map<String, String> params) {
        return null;
    }
}

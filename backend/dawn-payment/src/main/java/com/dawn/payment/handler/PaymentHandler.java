package com.dawn.payment.handler;

import java.util.Map;

public interface PaymentHandler {
    Boolean supports(String provider);

    String createPaymentUrl(String reservationId, Integer amount, String ipAddress);

    Boolean verifySignature(Map<String, String> params);

    String getId(Map<String, String> params);

    Boolean queryTransactions(String reservationId);
}

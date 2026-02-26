package com.dawn.payment.service;

import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;

import java.util.Map;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest req, String ip);

    PaymentHandlerResponse processCallback(String provider, Map<String, String> params);

    Boolean manualCheck(String provider, String id);
}

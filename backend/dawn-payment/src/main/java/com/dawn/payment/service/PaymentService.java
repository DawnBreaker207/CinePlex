package com.dawn.payment.service;

import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentRefundDTO;
import com.dawn.payment.dto.response.PaymentResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest req, String ip);

    PaymentHandlerResponse processCallback(String provider, Map<String, String> params);

    Boolean manualCheck(String provider, String id);

    void approveRefund(String reservationId);

    void rejectRefund(String reservationId);

    void markRefundPending(String reservationId);

    List<PaymentRefundDTO> listRefundRequests();

    List<PaymentRefundDTO> listFailedPayments();

    Optional<PaymentDetailDTO> findPaymentByReservationId(String reservationId);
}

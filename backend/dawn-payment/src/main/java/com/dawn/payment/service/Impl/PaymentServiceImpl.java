package com.dawn.payment.service.Impl;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.request.PaymentUpdateRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.dto.response.ReservationDTO;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.PaymentService;
import com.dawn.payment.service.ReservationClientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentHandler> handlers;

    private final PaymentRepository paymentRepository;

    private final ReservationClientService reservationClientService;

    public PaymentResponse createPayment(PaymentRequest req, String ip) {
        PaymentHandler handler = findHandler(req.getPaymentType());
        String url = handler.createPaymentUrl(req.getReservationId(), req.getAmount(), ip);

        return PaymentResponse
                .builder()
                .code("ok")
                .message("success")
                .paymentUrl(url)
                .build();
    }

    @Override
    @Transactional
    public PaymentHandlerResponse processCallback(String provider, Map<String, String> params) {
        PaymentHandler handler = findHandler(provider);
        String id = handler.getId(params);

        log.info("Handler payment: {}", handler);
        if (!handler.verifySignature(params)) {
            log.error("Error: wrong signature from {}", provider);
            reservationClientService.cancel(id);
            return PaymentHandlerResponse.builder()
                    .success(false)
                    .message("Invalid signature")
                    .build();
        }
        try {
            log.info("Process callback received {}", provider);
            ReservationDTO reservation = reservationClientService.confirm(id);
            log.info("Get reservation from payment {}", reservation);
            updatePayment(PaymentUpdateRequest
                    .builder()
                    .totalAmount(reservation.getTotalAmount())
                    .reservationId(id)
                    .method(checkPaymentMethod(provider))
                    .isSuccess(Boolean.TRUE)
                    .build());

            return PaymentHandlerResponse
                    .builder()
                    .reservationId(id)
                    .success(true)
                    .build();
        } catch (Exception ex) {
            log.info("Failed with id {}", id);
            return PaymentHandlerResponse
                    .builder()
                    .reservationId(id)
                    .success(false)
                    .message("Internal Error")
                    .build();
        }
    }

    @Override
    public Boolean manualCheck(String provider, String id) {

        PaymentHandler handler = findHandler(provider);
        if (handler.queryTransactions(id)) {
            log.info("Retry handler");
            return true;
        }
        return false;
    }

    @Transactional
    public void updatePayment(PaymentUpdateRequest request) {
        PaymentStatus status = request.getIsSuccess() ? PaymentStatus.PAID : PaymentStatus.CANCELED;
        paymentRepository.findByReservationId(request.getReservationId())
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .ifPresent((payment) -> {
                    throw new IllegalStateException(Message.Exception.PAYMENT_COMPLETE);
                });

        Payment payment = Payment
                .builder()
                .reservationId(request.getReservationId())
                .paymentIntentId(request.getReservationId())
                .method(request.getMethod())
                .amount(request.getTotalAmount())
                .status(status)
                .createdAt(Instant.now())
                .build();
        paymentRepository.saveAndFlush(payment);
    }

    private PaymentHandler findHandler(String provider) {
        return handlers
                .stream()
                .filter(h -> h.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Provider not supported"));
    }

    private PaymentMethod checkPaymentMethod(String provider) {
        if (provider == null) return PaymentMethod.UNKNOWN;

        String paymentMethod = provider.trim().toUpperCase();

        try {
            return PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown provider: {}. Default to UNKNOWN", paymentMethod);
            return PaymentMethod.UNKNOWN;
        }
    }
}

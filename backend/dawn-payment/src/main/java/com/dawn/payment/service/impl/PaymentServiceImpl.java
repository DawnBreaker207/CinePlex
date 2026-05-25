package com.dawn.payment.service.impl;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.exception.wrapper.ResourceAlreadyExistedException;
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

import java.math.BigDecimal;
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
        log.info("Creating payment with status: {}, method: {}",
                PaymentStatus.PENDING,
                checkPaymentMethod(req.getPaymentType()));
        Payment payment = Payment.builder()
                .reservationId(req.getReservationId())
                .amount(BigDecimal.valueOf(req.getAmount()))
                .method(checkPaymentMethod(req.getPaymentType()))
                .status(PaymentStatus.PENDING)
                .method(checkPaymentMethod(req.getPaymentType()))
                .paymentIntentId(req.getReservationId())
                .createdAt(Instant.now())
                .build();
        paymentRepository.save(payment);
        return PaymentResponse
                .builder()
                .code("ok")
                .message("success")
                .paymentUrl(url)
                .build();
    }

    @Override
    public PaymentHandlerResponse processCallback(String provider, Map<String, String> params) {
        PaymentHandler handler = findHandler(provider);
        String id = handler.getId(params);

        log.info("Handler payment: {}", handler);
        Payment existing = paymentRepository
                .findByReservationId(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PAYMENT_NOT_FOUND));

        if (PaymentStatus.PAID.equals(existing.getStatus())) {
            return PaymentHandlerResponse.builder()
                    .reservationId(id)
                    .success(true)
                    .build();
        }
        try {
            log.info("Process callback received {}", provider);
            // Save payment first
            updatePayment(PaymentUpdateRequest
                    .builder()
                    .reservationId(id)
                    .method(checkPaymentMethod(provider))
                    .isSuccess(true)
                    .build());

            // Save confirm reservation
            ReservationDTO reservation = reservationClientService.confirm(id);
            log.info("Get reservation from payment {}", reservation);

            // Update amount from reservation
            Payment payment = paymentRepository.findByReservationId(id).get();
            payment.setAmount(reservation.getTotalAmount());
            paymentRepository.save(payment);

            return PaymentHandlerResponse.builder()
                    .reservationId(id)
                    .success(true)
                    .build();
        } catch (Exception ex) {
            log.info("Failed with id {}", id);
            reservationClientService.cancel(id);
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

        Payment existing = paymentRepository
                .findByReservationId(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PAYMENT_NOT_FOUND));
        if (PaymentStatus.PAID.equals(existing.getStatus())) {
            throw new IllegalStateException(Message.Exception.PAYMENT_COMPLETE);
        }

        existing.setStatus(PaymentStatus.PAID);
        existing.setMethod(request.getMethod());
        paymentRepository.saveAndFlush(existing);
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

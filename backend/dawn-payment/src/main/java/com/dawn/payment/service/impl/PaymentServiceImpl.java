package com.dawn.payment.service.impl;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.PaymentService;
import com.dawn.payment.service.ReservationClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentHandler> handlers;

    private final PaymentRepository paymentRepository;

    private final ReservationClientService reservationClientService;

    private final RabbitTemplate rabbitTemplate;

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
        String reservationId = handler.getId(params);

        log.info("Handler payment: {}", handler);
        Payment existing = paymentRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PAYMENT_NOT_FOUND));

        if (PaymentStatus.PAID.equals(existing.getStatus())) {
            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(true)
                    .build();
        }
        try {
            log.info("Process callback received {}", provider);
            // Save payment first
            existing.setStatus(PaymentStatus.PAID);
            existing.setMethod(checkPaymentMethod(provider));
            existing.setCreatedAt(Instant.now());
            paymentRepository.saveAndFlush(existing);

            // Publish event
            PaymentCompletedEvent event = buildCompleteEvent(existing, provider);
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PAYMENT,
                    RabbitMQConstants.RK_PAYMENT_COMPLETED,
                    event);
            log.info("Published PaymentCompletedEvent for reservation: {}, eventId: {}",
                    reservationId, event.eventId());

            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(true)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to process callback for reservation: {}", reservationId, ex);
            PaymentFailedEvent failedEvent = PaymentFailedEvent
                    .builder()
                    .eventId(UUID.randomUUID().toString())
                    .reservationId(reservationId)
                    .reason(ex.getMessage())
                    .failedAt(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PAYMENT,
                    RabbitMQConstants.RK_PAYMENT_FAILED,
                    failedEvent);
            log.info("Published PaymentFailedEvent for reservation: {}", reservationId);

            return PaymentHandlerResponse
                    .builder()
                    .reservationId(reservationId)
                    .success(false)
                    .message(Message.Exception.PAYMENT_INTERNAL_ERROR)
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

    private PaymentHandler findHandler(String provider) {
        return handlers
                .stream()
                .filter(h -> h.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PROVIDER_NOT_SUPPORTED));
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

    private PaymentCompletedEvent buildCompleteEvent(Payment payment, String provider) {
        return PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .reservationId(payment.getReservationId())
                .amount(payment.getAmount())
                .method(checkPaymentMethod(provider))
                .paidAt(Instant.now())
                .build();
    }
}

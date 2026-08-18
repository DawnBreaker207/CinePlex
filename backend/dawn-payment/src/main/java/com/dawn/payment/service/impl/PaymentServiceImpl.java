package com.dawn.payment.service.impl;

import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Outbox;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.OutboxRepository;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.PaymentService;
import com.dawn.payment.service.ReservationClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String OUTBOX_EVENT_COMPLETED = "RESERVATION_CONFIRMED";

    private final List<PaymentHandler> handlers;

    private final PaymentRepository paymentRepository;

    private final ReservationClientService reservationClientService;

    private final RabbitTemplate rabbitTemplate;

    private final OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper;

    private final AuditLogService auditLogService;

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
                .paymentIntentId(req.getReservationId())
                .gatewayTxnRef(req.getReservationId())
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
    @Transactional
    public PaymentHandlerResponse processCallback(String provider, Map<String, String> params) {
        PaymentHandler handler = findHandler(provider);
        String reservationId = handler.getId(params);

        log.info("Handler payment: {}", handler);
        Payment existing = paymentRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PAYMENT_NOT_FOUND));

        if (!handler.verifySignature(params)) {
            log.warn("Invalid signature for callback of reservation {}", reservationId);
            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(false)
                    .message(Message.Exception.PAYMENT_INVALID_SIGNATURE)
                    .build();
        }

        // Idempotency: already PAID (possibly from a duplicate webhook) -> return old result
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
            existing.setGatewayTxnRef(handler.getTxnRef(params));
            existing.setPaidAt(Instant.now());
            paymentRepository.saveAndFlush(existing);

            // Write outbox row in the same transaction; OutboxPublisher sends it
            PaymentCompletedEvent event = buildCompleteEvent(existing, provider);
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize payment event", e);
            }
            outboxRepository.save(Outbox.builder()
                    .eventType(OUTBOX_EVENT_COMPLETED)
                    .reservationId(reservationId)
                    .payload(payload)
                    .build());
            log.info("Enqueued outbox event for reservation: {}, eventId: {}",
                    reservationId, event.eventId());
            auditLogService.record("PAYMENT_PAID", "PAYMENT", reservationId,
                    PaymentStatus.PENDING.name(), PaymentStatus.PAID.name(),
                    "provider=" + provider + ", txn=" + existing.getGatewayTxnRef());

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
            auditLogService.record("PAYMENT_FAILED", "PAYMENT", reservationId, null,
                    PaymentStatus.FAILED.name(), "provider=" + provider + ", reason=" + ex.getMessage());

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
        return paymentRepository
                .findByReservationId(id)
                .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                .orElse(false);
    }

    private PaymentHandler findHandler(String provider) {
        return handlers
                .stream()
                .filter(h -> h.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.PROVIDER_NOT_SUPPORTED));
    }

    @Override
    public Optional<PaymentDetailDTO> findPaymentByReservationId(String reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .map(p -> PaymentDetailDTO.builder()
                        .method(p.getMethod())
                        .status(p.getStatus())
                        .paymentIntentId(p.getPaymentIntentId())
                        .build());
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

package com.dawn.payment.service.impl;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxRepository;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentResponse;
import com.dawn.payment.handler.PaymentHandler;
import com.dawn.payment.model.Payment;
import com.dawn.payment.repository.PaymentRepository;
import com.dawn.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String OUTBOX_EVENT_FAILED = "PAYMENT_FAILED";

    private static final String AGGREGATE_PAYMENT = "payment";

    private final List<PaymentHandler> handlers;

    private final PaymentRepository paymentRepository;

    private final OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper;

    private final AuditLogService auditLogService;

    public PaymentResponse createPayment(PaymentRequest req, String ip) {
        PaymentHandler handler = findHandler(req.getPaymentType());
        String url = handler.createPaymentUrl(req.getReservationId(), req.getAmount(), ip);
        Payment payment = Payment.builder()
                .reservationId(req.getReservationId())
                .amount(BigDecimal.valueOf(req.getAmount()))
                .method(checkPaymentMethod(req.getPaymentType()))
                .status(PaymentStatus.PENDING)
                .paymentIntentId(req.getReservationId())
                .gatewayTxnRef(generateUniqueTxnRef())
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

    //  txnRef is UNIQUE (uk_payment_txn); a per-attempt token lets retry create a fresh payment row
    private String generateUniqueTxnRef() {
        String txnRef;
        do {
            txnRef = "CP" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (paymentRepository.findByGatewayTxnRef(txnRef).isPresent());
        return txnRef;
    }

    @Override
    @Transactional
    public PaymentHandlerResponse processCallback(String provider, Map<String, String> params) {
        PaymentHandler handler = findHandler(provider);
        String reservationId = handler.getId(params);

        Payment existing = paymentRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND.format()));

        log.info("Callback received from {} for reservation {}: {}", provider, reservationId, params);

        if (!handler.verifySignature(params)) {
            log.warn("Invalid signature for callback of reservation {}, full payload: {}", reservationId, params);
            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(false)
                    .message(ErrorCode.PAYMENT_INVALID_SIGNATURE.format())
                    .build();
        }

        // Idempotency: already PAID (possibly from a duplicate webhook) -> return old result
        if (PaymentStatus.PAID.equals(existing.getStatus())) {
            checkContradictoryCallback(handler, provider, params, existing, reservationId);
            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(true)
                    .build();
        }

        //  Amount verification: gateway-signed amount must match what we charged
        BigDecimal gatewayAmount = handler.getAmount(params);
        if (gatewayAmount != null && existing.getAmount().compareTo(gatewayAmount) != 0) {
            log.warn("Callback amount mismatch for reservation {}: gateway={}, ours={}", reservationId, gatewayAmount, existing.getAmount());
            existing.setStatus(PaymentStatus.FAILED);
            existing.setStatusReason("AMOUNT_MISMATCH");
            existing.setLastError("gateway=" + gatewayAmount + ", expected=" + existing.getAmount());
            existing.setCheckedAt(Instant.now());
            paymentRepository.save(existing);
            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(false)
                    .message(ErrorCode.PAYMENT_AMOUNT_MISMATCH.format())
                    .build();
        }
        try {
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
            // Idempotency: a concurrent callback may have enqueued this event already
            if (outboxRepository.existsByEventTypeAndAggregateId(OUTBOX_EVENT_COMPLETED, reservationId)) {
                log.info("Outbox event already enqueued for reservation {}, skipping", reservationId);
            } else {
                outboxRepository.save(Outbox.builder()
                        .aggregateType(AGGREGATE_PAYMENT)
                        .aggregateId(reservationId)
                        .eventType(OUTBOX_EVENT_COMPLETED)
                        .exchange(RabbitMQConstants.EXCHANGE_PAYMENT)
                        .routingKey(RabbitMQConstants.RK_PAYMENT_COMPLETED)
                        .payload(payload)
                        .build());
                log.info("Enqueued outbox event for reservation: {}, eventId: {}",
                        reservationId, event.eventId());
            }
            auditLogService.record("PAYMENT_PAID", "PAYMENT", reservationId, null,
                    PaymentStatus.PENDING.name(), PaymentStatus.PAID.name(),
                    "provider=" + provider + ", txn=" + existing.getGatewayTxnRef(),
                    "SUCCESS", AuditLogService.clientIp(), null, null);

            return PaymentHandlerResponse.builder()
                    .reservationId(reservationId)
                    .success(true)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to process callback for reservation: {}", reservationId, ex);
            existing.setStatus(PaymentStatus.FAILED);
            existing.setStatusReason("CALLBACK_FAILURE");
            existing.setLastError(ex.getMessage());
            existing.setCheckedAt(Instant.now());
            paymentRepository.save(existing);
            PaymentFailedEvent failedEvent = PaymentFailedEvent
                    .builder()
                    .eventId(UUID.randomUUID().toString())
                    .reservationCode(reservationId)
                    .reason(ex.getMessage())
                    .failedAt(Instant.now())
                    .build();

            String failedPayload;
            try {
                failedPayload = objectMapper.writeValueAsString(failedEvent);
            } catch (JsonProcessingException je) {
                throw new IllegalStateException("Failed to serialize payment failed event", je);
            }
            outboxRepository.save(Outbox.builder()
                    .aggregateType(AGGREGATE_PAYMENT)
                    .aggregateId(reservationId)
                    .eventType(OUTBOX_EVENT_FAILED)
                    .exchange(RabbitMQConstants.EXCHANGE_PAYMENT)
                    .routingKey(RabbitMQConstants.RK_PAYMENT_FAILED)
                    .payload(failedPayload)
                    .build());
            log.info("Enqueued outbox event for reservation: {}, eventId: {}", reservationId, failedEvent.eventId());
            auditLogService.record("PAYMENT_FAILED", "PAYMENT", reservationId, null,
                    null, PaymentStatus.FAILED.name(), "provider=" + provider + ", reason=" + ex.getMessage(),
                    "FAILED", AuditLogService.clientIp(), null, null);

            return PaymentHandlerResponse
                    .builder()
                    .reservationId(reservationId)
                    .success(false)
                    .message(ErrorCode.PAYMENT_INTERNAL_ERROR.format())
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
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROVIDER_NOT_SUPPORTED.format()));
    }

    private void checkContradictoryCallback(PaymentHandler handler, String provider,
            Map<String, String> params, Payment existing, String reservationId) {
        try {
            BigDecimal gatewayAmount = handler.getAmount(params);
            String gatewayTxnRef = handler.getTxnRef(params);
            boolean amountDiffers = gatewayAmount != null
                    && existing.getAmount().compareTo(gatewayAmount) != 0;
            boolean txnDiffers = gatewayTxnRef != null
                    && !gatewayTxnRef.equals(existing.getGatewayTxnRef());
            if (amountDiffers || txnDiffers) {
                log.warn("Contradictory callback for PAID reservation {}: amount gateway={} ours={}, txn gateway={} ours={}",
                        reservationId, gatewayAmount, existing.getAmount(),
                        gatewayTxnRef, existing.getGatewayTxnRef());
                auditLogService.record("PAYMENT_CALLBACK_CONFLICT", "PAYMENT", reservationId, null,
                        PaymentStatus.PAID.name(), PaymentStatus.PAID.name(),
                        "provider=" + provider + ", gatewayAmount=" + gatewayAmount
                                + ", gatewayTxn=" + gatewayTxnRef,
                        "WARNING", AuditLogService.clientIp(), null, null);
            }
        } catch (RuntimeException e) {
            log.warn("Contradiction check failed for reservation {}", reservationId, e);
        }
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
                .reservationCode(payment.getReservationId())
                .amount(payment.getAmount())
                .method(checkPaymentMethod(provider))
                .paidAt(Instant.now())
                .build();
    }
}

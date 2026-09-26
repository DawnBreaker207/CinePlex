package com.dawn.payment.service.impl;

import com.dawn.booking.api.BookingModuleApi;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import com.dawn.common.core.constant.PermissionKey;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxRepository;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.SecurityUtils;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.payment.dto.request.PaymentRequest;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.dto.response.PaymentHandlerResponse;
import com.dawn.payment.dto.response.PaymentRefundDTO;
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

    private final IdentityModuleApi identityModuleApi;

    private final AuditLogService auditLogService;

    private final BookingModuleApi bookingApi;

    // S4 guard + C0 dedup + C4 snapshot in one place: the money entry point.
    public PaymentResponse createPayment(PaymentRequest req, String ip) {
        // S4: refuse to send the user to the gateway with a dead hold
        // (laptop sleep / expired TTL / abandoned session). FE catches 409
        // RESERVATION_EXPIRED and re-inits a fresh reservation instead of
        // paying into a hold that can never confirm.
        Long holdTtl = bookingApi.getHoldTtlSeconds(req.getReservationId());
        if (holdTtl == null || holdTtl <= 0) {
            log.warn("Payment refused for reservation {}: hold already expired", req.getReservationId());
            throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                    ErrorCode.RESERVATION_EXPIRED.format());
        }

        // C0: a retried / double-clicked / second-device payment must reuse the
        // open row instead of inserting a duplicate (duplicates used to 500
        // the gateway callback via single-result findByReservationId).
        Optional<Payment> open = paymentRepository
                .findFirstByReservationIdOrderByCreatedAtDesc(req.getReservationId());
        if (open.isPresent()) {
            Payment existing = open.get();
            if (PaymentStatus.PAID.equals(existing.getStatus())) {
                throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                        ErrorCode.PAYMENT_COMPLETE.format());
            }
            if (PaymentStatus.PENDING.equals(existing.getStatus())) {
                PaymentHandler handler = findHandler(req.getPaymentType());
                String url = handler.createPaymentUrl(req.getReservationId(), req.getAmount(), ip);
                log.info("Reusing open payment row {} for reservation {}",
                        existing.getId(), req.getReservationId());
                return PaymentResponse.builder()
                        .code("ok")
                        .message("success")
                        .paymentUrl(url)
                        .build();
            }
            // FAILED / REFUNDED / others: fall through and open a fresh attempt.
        }

        PaymentHandler handler = findHandler(req.getPaymentType());
        String url = handler.createPaymentUrl(req.getReservationId(), req.getAmount(), ip);
        Payment payment = Payment.builder()
                .reservationId(req.getReservationId())
                .amount(BigDecimal.valueOf(req.getAmount()))
                .method(checkPaymentMethod(req.getPaymentType()))
                .status(PaymentStatus.PENDING)
                .paymentIntentId(req.getReservationId())
                .gatewayTxnRef(generateUniqueTxnRef())
                .seatSnapshot(serializeSeatIds(req.getSeatIds()))
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

    // C4 snapshot: seats being paid for, carried into the completed event for
    // post-payment reconciliation. Null-safe: legacy clients omit seatIds.
    private String serializeSeatIds(java.util.List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(seatIds);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize seat snapshot, reconciliation skipped: {}", e.getMessage());
            return null;
        }
    }

    private java.util.List<Long> parseSeatIds(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Long>>() {
                    });
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse seat snapshot, reconciliation skipped: {}", e.getMessage());
            return null;
        }
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

        // C0: newest row wins (failed retries may leave older rows behind);
        // single-result findBy used to 500 here on duplicates.
        Payment existing = paymentRepository
                .findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
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
                .findFirstByReservationIdOrderByCreatedAtDesc(id)
                .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                .orElse(false);
    }

    @Override
    @Transactional
    public void markRefundPending(String reservationId) {
        paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(reservationId).ifPresent(payment -> {
            if (PaymentStatus.PAID.equals(payment.getStatus())) {
                payment.setStatus(PaymentStatus.REFUND_PENDING);
                paymentRepository.save(payment);
                log.info("Payment marked REFUND_PENDING for reservation {}", reservationId);
            }
        });
    }

    @Override
    @Transactional
    public void approveRefund(String reservationId) {
        Long approverId = SecurityUtils.getCurrentUserId();
        if (!identityModuleApi.hasPermission(approverId, PermissionKey.FINANCE)) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH);
        }
        Payment payment = paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND.format()));
        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new InvalidRequestException(ErrorCode.INVALID_STATE_TRANSITION,
                    payment.getStatus(), PaymentStatus.REFUNDED);
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        auditLogService.record("REFUND_APPROVED", "PAYMENT", reservationId, null,
                PaymentStatus.REFUND_PENDING.name(), PaymentStatus.REFUNDED.name(),
                "approver=" + approverId, "SUCCESS", AuditLogService.clientIp(), null, null);
    }

    @Override
    public List<PaymentRefundDTO> listRefundRequests() {
        Long currentId = SecurityUtils.getCurrentUserId();
        if (!identityModuleApi.hasPermission(currentId, PermissionKey.FINANCE)) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH);
        }
        return paymentRepository.findAllByStatusOrderByCreatedAtDesc(PaymentStatus.REFUND_PENDING).stream()
                .map(this::toQueueDTO)
                .toList();
    }

    @Override
    public List<PaymentRefundDTO> listFailedPayments() {
        Long currentId = SecurityUtils.getCurrentUserId();
        if (!identityModuleApi.hasPermission(currentId, PermissionKey.FINANCE)) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH);
        }
        return paymentRepository.findAllByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED).stream()
                .map(this::toQueueDTO)
                .toList();
    }

    private PaymentRefundDTO toQueueDTO(Payment p) {
        return PaymentRefundDTO.builder()
                .reservationId(p.getReservationId())
                .amount(p.getAmount())
                .method(p.getMethod())
                .status(p.getStatus())
                .statusReason(p.getStatusReason())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void rejectRefund(String reservationId) {
        Long approverId = SecurityUtils.getCurrentUserId();
        if (!identityModuleApi.hasPermission(approverId, PermissionKey.FINANCE)) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_NOT_ENOUGH);
        }
        Payment payment = paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND.format()));
        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new InvalidRequestException(ErrorCode.INVALID_STATE_TRANSITION,
                    payment.getStatus(), PaymentStatus.PAID);
        }
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        auditLogService.record("REFUND_REJECTED", "PAYMENT", reservationId, null,
                PaymentStatus.REFUND_PENDING.name(), PaymentStatus.PAID.name(),
                "approver=" + approverId, "SUCCESS", AuditLogService.clientIp(), null, null);
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
        return paymentRepository.findFirstByReservationIdOrderByCreatedAtDesc(reservationId)
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
                .seatIds(parseSeatIds(payment.getSeatSnapshot()))
                .amount(payment.getAmount())
                .method(checkPaymentMethod(provider))
                .paidAt(Instant.now())
                .build();
    }
}

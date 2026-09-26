package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import com.dawn.common.core.dto.event.RefundRequestedEvent;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ReservationExpiredException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCompletedListener {
    private final ReservationService reservationService;
    private final ReservationRedisService reservationRedisService;
    private final RabbitTemplate rabbitTemplate;

    // States from which confirm() can never succeed. Rethrowing here
    // redelivers forever (poison-message hot loop) — settle and ACK instead.
    private static final Set<ReservationStatus> TERMINAL = EnumSet.of(
            ReservationStatus.EXPIRED,
            ReservationStatus.CANCELED,
            ReservationStatus.REFUNDED,
            ReservationStatus.FAILED);

    @RabbitListener(queues = RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED,
            containerFactory = "paymentListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.debug("[Booking] PaymentCompletedEvent received: reservationCode={}, eventId={}",
                event.reservationCode(), event.eventId());
        try {
            reconcilePaidSeats(event);
            reservationService.confirm(event.reservationCode());
            log.debug("[Booking] Reservation {} confirmed successfully", event.reservationCode());
        } catch (Exception e) {
            if (isPermanentFailure(event.reservationCode(), e)) {
                settleUnconfirmable(event.reservationCode(), e);
                return;
            }
            log.error("[Booking] Transient failure confirming reservation {}: {}",
                    event.reservationCode(), e.getMessage());
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED,
            containerFactory = "paymentListenerContainerFactory")
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.debug("[Booking] PaymentFailedEvent received: reservationCode={}, reason={}",
                event.reservationCode(), event.reason());
        try {
            reservationService.fail(event.reservationCode());
            log.debug("[Booking] Reservation {} failed successfully", event.reservationCode());
        } catch (Exception e) {
            if (isPermanentFailure(event.reservationCode(), e)) {
                settleUnconfirmable(event.reservationCode(), e);
                return;
            }
            log.error("[Booking] Transient failure failing reservation {}: {}",
                    event.reservationCode(), e.getMessage());
            throw e;
        }
    }

    // C4: the seats paid for must still be the seats held. A seat change
    // between createPayment and the gateway callback used to be confirmed
    // silently (wrong seats, wrong total). Mismatch is unrecoverable.
    private void reconcilePaidSeats(PaymentCompletedEvent event) {
        List<Long> paidSeats = event.seatIds();
        if (paidSeats == null || paidSeats.isEmpty()) {
            return; // Legacy client without snapshot: confirm as before.
        }
        List<Long> currentSeats;
        try {
            ReservationRedisDTO session =
                    reservationRedisService.getReservationSession(event.reservationCode());
            currentSeats = session.getSeatsIds() != null ? session.getSeatsIds() : List.of();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.PAYMENT_SEAT_MISMATCH, HttpStatus.CONFLICT,
                    "hold lost after payment");
        }
        if (!new HashSet<>(paidSeats).equals(new HashSet<>(currentSeats))) {
            log.warn("[Booking] Seat mismatch for {}: paid={}, held={}",
                    event.reservationCode(), paidSeats, currentSeats);
            throw new ApiException(ErrorCode.PAYMENT_SEAT_MISMATCH, HttpStatus.CONFLICT);
        }
    }

    // Permanent = this event can never confirm, no matter how often it retries.
    // Settle it: release the hold promptly (no-op when already terminal),
    // enqueue a refund request when the gateway took money, then ACK.
    private boolean isPermanentFailure(String reservationCode, Exception e) {
        Optional<ReservationStatus> status = reservationService.findDetailByCode(reservationCode)
                .map(d -> d.getReservationStatus());
        if (status.isEmpty()) {
            return true; // Unknown reservation: nothing will ever confirm.
        }
        if (status.get() == ReservationStatus.CONFIRMED) {
            return true; // Duplicate event: already confirmed, nothing to do.
        }
        if (TERMINAL.contains(status.get())) {
            return true;
        }
        // Still PENDING: only hold/seat/object-driven failures are unrecoverable.
        // Infra errors (Redis/DB) and busy-processing stay transient -> retry.
        return e instanceof SeatUnavailableException
                || e instanceof ReservationExpiredException
                || e instanceof ResourceNotFoundException
                || e instanceof IllegalStateException
                || (e instanceof ApiException api && api.getErrorCode() == ErrorCode.PAYMENT_SEAT_MISMATCH)
                || (e instanceof ApiException api && api.getErrorCode() == ErrorCode.RESERVATION_INVALID_STATUS);
    }

    private void settleUnconfirmable(String reservationCode, Exception cause) {
        log.warn("[Booking] Settling unconfirmable reservation {}: {}",
                reservationCode, cause.getMessage());
        try {
            // Prompt release; no-op when already in a final state.
            reservationService.fail(reservationCode);
        } catch (Exception e) {
            log.warn("[Booking] Release skipped for {}: {}", reservationCode, e.getMessage());
        }
        try {
            // Paid-but-unconfirmable becomes a refund request. markRefundPending
            // only acts when the payment row is PAID, so this is a safe no-op
            // for unpaid/duplicate events.
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PAYMENT,
                    RabbitMQConstants.RK_PAYMENT_REFUND_REQUESTED,
                    RefundRequestedEvent.builder()
                            .reservationCode(reservationCode)
                            .build());
            log.info("[Booking] Refund requested for unconfirmable reservation {}", reservationCode);
        } catch (Exception e) {
            log.error("[Booking] Failed to request refund for {}", reservationCode, e);
            throw e;
        }
    }
}

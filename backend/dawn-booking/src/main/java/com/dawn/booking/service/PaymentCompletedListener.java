package com.dawn.booking.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.dto.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCompletedListener {
    private final ReservationService reservationService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.debug("[Booking] PaymentCompletedEvent received: reservationCode={}, eventId={}",
                event.reservationCode(), event.eventId());
        try {
            reservationService.confirm(event.reservationCode());
            log.debug("[Booking] Reservation {} confirmed successfully", event.reservationCode());
        } catch (Exception e) {
            log.error("[Booking] Failed to confirm reservation {}: {}", event.reservationCode(), e.getMessage());
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.debug("[Booking] PaymentFailedEvent received: reservationCode={}, reason={}",
                event.reservationCode(), event.reason());
        try {
            reservationService.fail(event.reservationCode());
            log.debug("[Booking] Reservation {} failed successfully", event.reservationCode());
        } catch (Exception e) {
            log.error("[Booking] Failed to fail reservation {}: {}", event.reservationCode(), e.getMessage());
            throw e;
        }
    }
}

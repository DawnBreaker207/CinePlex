package com.dawn.notification.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.BookingCompleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentNotificationListener {
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFICATION_RESERVATION_COMPLETED)
    public void handleBookingComplete(BookingCompleteEvent event) {
        log.info("Received booking event for: {}", event.reservationId());
        try {
            emailService.sendReservationEmail(event);
            log.info("[Notification] Confirmation email sent for reservation: {}", event.reservationId());
        } catch (Exception e) {
            log.error("[Notification] Failed to send email for reservation {}: {}",
                    event.reservationId(), e.getMessage());
            throw e;
        }

    }
}

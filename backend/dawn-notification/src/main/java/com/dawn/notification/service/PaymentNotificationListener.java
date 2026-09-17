package com.dawn.notification.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.BookingCompleteEvent;
import com.dawn.common.core.event.ProcessedEvent;
import com.dawn.common.core.event.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentNotificationListener {
    private final EmailService emailService;
    private final ProcessedEventRepository dedupRepository;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFICATION_RESERVATION_COMPLETED)
    @Transactional
    public void handleBookingComplete(BookingCompleteEvent event) {
        String eventId = event.reservationCode();
        if (dedupRepository.existsById(eventId)) {
            log.info("Skipping duplicate notification event for reservation: {}", eventId);
            return;
        }
        log.info("Received booking event for: {}", eventId);
        try {
            emailService.sendReservationEmail(event);
            dedupRepository.save(ProcessedEvent.builder().eventId(eventId).build());
            log.info("[Notification] Confirmation email sent for reservation: {}", eventId);
        } catch (DataIntegrityViolationException e) {
            log.info("Notification event {} already processed concurrently, skipping", eventId);
        } catch (Exception e) {
            log.error("[Notification] Failed to send email for reservation {}: {}",
                    eventId, e.getMessage(), e);
            throw e;
        }
    }
}

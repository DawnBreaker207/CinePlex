package com.dawn.notification.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.ShowtimeCanceledMailEvent;
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
public class ShowtimeCanceledMailListener {

    private final EmailService emailService;
    private final ProcessedEventRepository dedupRepository;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFICATION_SHOWTIME_CANCELED)
    @Transactional
    public void handleShowtimeCanceled(ShowtimeCanceledMailEvent event) {
        String eventId = "showtime-canceled:" + event.reservationCode();
        if (dedupRepository.existsById(eventId)) {
            log.info("Skipping duplicate showtime-canceled mail for reservation: {}", event.reservationCode());
            return;
        }
        try {
            emailService.sendShowtimeCanceledEmail(event.to(), event.movieTitle(),
                    event.showtimeInfo(), event.reservationCode(), event.wasPaid());
            dedupRepository.save(ProcessedEvent.builder().eventId(eventId).build());
            log.info("[Notification] Showtime-canceled email sent for reservation: {}", event.reservationCode());
        } catch (DataIntegrityViolationException e) {
            log.info("Showtime-canceled mail {} already processed concurrently, skipping", eventId);
        } catch (Exception e) {
            log.error("[Notification] Failed to send showtime-canceled email for reservation {}: {}",
                    event.reservationCode(), e.getMessage(), e);
            throw e;
        }
    }
}

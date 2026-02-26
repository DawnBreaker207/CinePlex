package com.dawn.notification.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.request.BookingNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitListenerNotify {

    private final SimpMessagingTemplate messagingTemplate;

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFY)
    public void handleBookingEvent(BookingNotificationEvent event) {
        log.info("Received booking event for: {}", event.getReservationId());
        emailService.sendReservationEmail(event);
    }

    @RabbitListener(queues = RabbitMQConstants.QUEUE_DASHBOARD)
    public void handleUpdateDashboard() {
        Map<String, String> payload = Collections.singletonMap("action", "REFRESH");
        messagingTemplate.convertAndSend("/topic/dashboard/update", payload);
    }
}

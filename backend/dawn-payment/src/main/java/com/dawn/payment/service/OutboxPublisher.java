package com.dawn.payment.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.model.Outbox;
import com.dawn.payment.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_ATTEMPTS = 5;

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:5000}")
    public void publishPending() {
        List<Outbox> pending = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) {
            return;
        }
        for (Outbox outbox : pending) {
            try {
                PaymentCompletedEvent event = objectMapper.readValue(outbox.getPayload(), PaymentCompletedEvent.class);
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.EXCHANGE_PAYMENT,
                        RabbitMQConstants.RK_PAYMENT_COMPLETED,
                        event);
                outbox.setStatus("SENT");
                outboxRepository.save(outbox);
                auditLogService.record("OUTBOX_PUBLISHED", "OUTBOX", String.valueOf(outbox.getId()), null, "SENT",
                        "reservationId=" + outbox.getReservationId());
                log.info("Outbox {} published for reservation {}", outbox.getId(), outbox.getReservationId());
            } catch (Exception e) {
                outbox.setAttempts(outbox.getAttempts() + 1);
                outbox.setLastError(e.getMessage());
                if (outbox.getAttempts() >= MAX_ATTEMPTS) {
                    outbox.setStatus("FAILED");
                    auditLogService.record("OUTBOX_FAILED", "OUTBOX", String.valueOf(outbox.getId()), null, "FAILED",
                            "reservationId=" + outbox.getReservationId() + ", attempts=" + outbox.getAttempts());
                    log.error("Outbox {} permanently failed after {} attempts", outbox.getId(), outbox.getAttempts(), e);
                } else {
                    log.warn("Outbox {} publish failed (attempt {}): {}", outbox.getId(), outbox.getAttempts(), e.getMessage());
                }
                outboxRepository.save(outbox);
            }
        }
    }
}
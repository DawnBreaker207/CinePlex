package com.dawn.common.core.outbox;

import com.dawn.common.core.service.AuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
                JsonNode payload = objectMapper.readTree(outbox.getPayload());
                rabbitTemplate.convertAndSend(outbox.getExchange(), outbox.getRoutingKey(), payload);
                outbox.setStatus("SENT");
                outboxRepository.save(outbox);
                auditLogService.record("OUTBOX_PUBLISHED", "OUTBOX", String.valueOf(outbox.getId()), null, null, "SENT",
                        "aggregateType=" + outbox.getAggregateType() + ", aggregateId=" + outbox.getAggregateId(),
                        "SUCCESS", AuditLogService.clientIp(), null, null);
                log.info("Outbox {} published for {} {}", outbox.getId(), outbox.getAggregateType(), outbox.getAggregateId());
            } catch (ObjectOptimisticLockingFailureException e) {
                // Another publisher instance already claimed this row; do not bump attempts
                log.info("Outbox {} already claimed by another publisher, skipping", outbox.getId());
            } catch (Exception e) {
                outbox.setAttempts(outbox.getAttempts() + 1);
                outbox.setLastError(e.getMessage());
                if (outbox.getAttempts() >= MAX_ATTEMPTS) {
                    outbox.setStatus("FAILED");
                    auditLogService.record("OUTBOX_FAILED", "OUTBOX", String.valueOf(outbox.getId()), null, null, "FAILED",
                            "aggregateType=" + outbox.getAggregateType() + ", aggregateId=" + outbox.getAggregateId()
                                    + ", attempts=" + outbox.getAttempts(),
                            "FAILED", AuditLogService.clientIp(), null, null);
                    log.error("Outbox {} permanently failed after {} attempts", outbox.getId(), outbox.getAttempts(), e);
                } else {
                    log.warn("Outbox {} publish failed (attempt {}): {}", outbox.getId(), outbox.getAttempts(), e.getMessage());
                }
                outboxRepository.save(outbox);
            }
        }
    }
}
package com.dawn.payment.service;

import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxRepository;
import com.dawn.common.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJob {

    private static final long STALE_MINUTES = 10;
    // FAILED rows get resurrected to PENDING (attempts reset) this many times before
    // becoming a true dead letter that only manual intervention can fix.
    private static final int MAX_RETRY_CYCLES = 3;

    private final OutboxRepository outboxRepository;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelayString = "${app.reconciliation.scan-interval-ms:300000}")
    public void alertStaleOutbox() {
        Instant cutoff = Instant.now().minus(STALE_MINUTES, ChronoUnit.MINUTES);
        List<Outbox> stale = outboxRepository.findByStatusInAndUpdatedAtBefore(List.of("PENDING", "FAILED"), cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (Outbox outbox : stale) {
            if ("FAILED".equals(outbox.getStatus()) && outbox.getRetryCycle() < MAX_RETRY_CYCLES) {
                resurrect(outbox);
                continue;
            }
            log.warn("Outbox {} for aggregate {} stuck in {} (attempts={}, retryCycle={}, lastError={}), manual intervention required",
                    outbox.getId(), outbox.getAggregateId(), outbox.getStatus(), outbox.getAttempts(),
                    outbox.getRetryCycle(), outbox.getLastError());
            auditLogService.record("RECONCILIATION_ALERT", "OUTBOX", String.valueOf(outbox.getId()), null, null, outbox.getStatus(),
                    "aggregateType=" + outbox.getAggregateType() + ", aggregateId=" + outbox.getAggregateId()
                            + ", attempts=" + outbox.getAttempts(),
                    "SUCCESS", AuditLogService.clientIp(), null, null);
        }
    }

    private void resurrect(Outbox outbox) {
        outbox.setStatus("PENDING");
        outbox.setAttempts(0);
        outbox.setRetryCycle(outbox.getRetryCycle() + 1);
        outboxRepository.save(outbox);
        log.warn("Outbox {} for aggregate {} resurrected from FAILED (retryCycle={}/{})",
                outbox.getId(), outbox.getAggregateId(), outbox.getRetryCycle(), MAX_RETRY_CYCLES);
        auditLogService.record("OUTBOX_RESURRECTED", "OUTBOX", String.valueOf(outbox.getId()), null, "FAILED", "PENDING",
                "aggregateType=" + outbox.getAggregateType() + ", aggregateId=" + outbox.getAggregateId()
                        + ", retryCycle=" + outbox.getRetryCycle(),
                "SUCCESS", AuditLogService.clientIp(), null, null);
    }
}

package com.dawn.payment.service;

import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.model.Outbox;
import com.dawn.payment.repository.OutboxRepository;
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
            log.warn("Outbox {} for reservation {} stuck in {} (attempts={}, lastError={}), manual intervention required",
                    outbox.getId(), outbox.getReservationId(), outbox.getStatus(), outbox.getAttempts(), outbox.getLastError());
            auditLogService.record("RECONCILIATION_ALERT", "OUTBOX", String.valueOf(outbox.getId()), null, outbox.getStatus(),
                    "reservationId=" + outbox.getReservationId() + ", attempts=" + outbox.getAttempts());
        }
    }
}

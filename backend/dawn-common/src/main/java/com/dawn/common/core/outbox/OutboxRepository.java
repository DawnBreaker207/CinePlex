package com.dawn.common.core.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findTop100ByStatusOrderByCreatedAtAsc(String status);

    boolean existsByEventTypeAndAggregateId(String eventType, String aggregateId);

    List<Outbox> findByStatusInAndUpdatedAtBefore(List<String> statuses, Instant cutoff);
}
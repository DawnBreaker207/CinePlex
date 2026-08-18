package com.dawn.payment.repository;

import com.dawn.payment.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findTop100ByStatusOrderByCreatedAtAsc(String status);

    List<Outbox> findByStatusInAndUpdatedAtBefore(List<String> statuses, Instant cutoff);
}
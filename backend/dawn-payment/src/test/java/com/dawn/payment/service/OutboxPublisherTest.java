package com.dawn.payment.service;

import com.dawn.common.core.outbox.Outbox;
import com.dawn.common.core.outbox.OutboxPublisher;
import com.dawn.common.core.outbox.OutboxRepository;
import com.dawn.common.core.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher")
class OutboxPublisherTest {

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    AuditLogService auditLogService;

    OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxRepository, rabbitTemplate, new ObjectMapper(), auditLogService);
    }

    @Test
    @DisplayName("publish pending outbox  send event, mark SENT")
    void publishPending_shouldSendAndMarkSent() throws Exception {
        Outbox outbox = Outbox.builder()
                .id(1L)
                .aggregateType("payment")
                .aggregateId("RES-001")
                .eventType("RESERVATION_CONFIRMED")
                .exchange("payment.exchange")
                .routingKey("payment.completed")
                .payload("{\"eventId\":\"EVT-1\",\"reservationCode\":\"RES-001\"}")
                .build();
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(outbox));

        publisher.publishPending();

        verify(rabbitTemplate).convertAndSend("payment.exchange", "payment.completed",
                new ObjectMapper().readTree(outbox.getPayload()));
        verify(outboxRepository).save(outbox);
        assertThat(outbox.getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("row claimed by another publisher (optimistic lock)  skip, no attempt bump")
    void publishPending_staleVersion_shouldSkipWithoutBumpingAttempts() throws Exception {
        Outbox outbox = Outbox.builder()
                .id(1L)
                .aggregateType("payment")
                .aggregateId("RES-001")
                .eventType("RESERVATION_CONFIRMED")
                .exchange("payment.exchange")
                .routingKey("payment.completed")
                .payload("{\"eventId\":\"EVT-1\"}")
                .build();
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(outbox));
        when(outboxRepository.save(outbox))
                .thenThrow(new ObjectOptimisticLockingFailureException(Outbox.class.getName(), 1L));

        assertThatCode(() -> publisher.publishPending()).doesNotThrowAnyException();

        assertThat(outbox.getAttempts()).isZero();
        assertThat(outbox.getStatus()).isNotEqualTo("FAILED");
    }
}
package com.dawn.payment.service;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.dto.event.PaymentCompletedEvent;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.payment.model.Outbox;
import com.dawn.payment.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    ObjectMapper objectMapper;

    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    OutboxPublisher publisher;

    @Test
    @DisplayName("publish pending outbox  send event, mark SENT")
    void publishPending_shouldSendAndMarkSent() throws Exception {
        Outbox outbox = Outbox.builder()
                .id(1L)
                .eventType("RESERVATION_CONFIRMED")
                .reservationId("RES-001")
                .payload("{}")
                .build();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId("EVT-1")
                .reservationCode("RES-001")
                .userId(1L)
                .theaterId(5L)
                .seatIds(List.of(1L))
                .amount(new BigDecimal("100000"))
                .method(PaymentMethod.VNPAY)
                .paidAt(Instant.now())
                .build();
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(outbox));
        when(objectMapper.readValue(anyString(), eq(PaymentCompletedEvent.class))).thenReturn(event);

        publisher.publishPending();

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(event));
        verify(outboxRepository).save(outbox);
        assertThat(outbox.getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("row claimed by another publisher (optimistic lock)  skip, no attempt bump")
    void publishPending_staleVersion_shouldSkipWithoutBumpingAttempts() throws Exception {
        Outbox outbox = Outbox.builder()
                .id(1L)
                .eventType("RESERVATION_CONFIRMED")
                .reservationId("RES-001")
                .payload("{}")
                .build();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId("EVT-1")
                .reservationCode("RES-001")
                .userId(1L)
                .theaterId(5L)
                .seatIds(List.of(1L))
                .amount(new BigDecimal("100000"))
                .method(PaymentMethod.VNPAY)
                .paidAt(Instant.now())
                .build();
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(outbox));
        when(objectMapper.readValue(anyString(), eq(PaymentCompletedEvent.class))).thenReturn(event);
        when(outboxRepository.save(outbox))
                .thenThrow(new ObjectOptimisticLockingFailureException(Outbox.class.getName(), 1L));

        assertThatCode(() -> publisher.publishPending()).doesNotThrowAnyException();

        assertThat(outbox.getAttempts()).isZero();
        assertThat(outbox.getStatus()).isNotEqualTo("FAILED");
    }
}

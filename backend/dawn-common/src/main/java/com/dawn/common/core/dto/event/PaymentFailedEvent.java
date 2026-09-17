package com.dawn.common.core.dto.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentFailedEvent(
        String eventId,
        String reservationCode,
        String reason,
        Instant failedAt) {
}

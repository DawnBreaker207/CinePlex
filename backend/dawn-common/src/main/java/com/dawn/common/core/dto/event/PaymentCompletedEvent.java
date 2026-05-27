package com.dawn.common.core.dto.event;

import com.dawn.common.core.constant.PaymentMethod;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record PaymentCompletedEvent(
        String eventId,
        String reservationId,
        Long userId,
        Long theaterId,
        List<Long> seatIds,
        String voucherCode,
        BigDecimal amount,
        PaymentMethod method,
        Instant paidAt
) {
}

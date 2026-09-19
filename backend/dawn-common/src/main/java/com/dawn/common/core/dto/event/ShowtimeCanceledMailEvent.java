package com.dawn.common.core.dto.event;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record ShowtimeCanceledMailEvent(
        String to,
        String movieTitle,
        String showtimeInfo,
        String reservationCode,
        boolean wasPaid
) implements Serializable {
}

package com.dawn.common.core.dto.event;

import lombok.Builder;

import java.io.Serializable;


@Builder
public record BookingCompleteEvent(
        String to,
        String name,
        String reservationId,
        String movieName,
        String theaterName,
        String showtimeSession,
        String seats,
        String paymentTime,
        String total
) implements Serializable {
}

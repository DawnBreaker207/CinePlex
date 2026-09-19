package com.dawn.common.core.dto.event;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record RefundRequestedEvent(
        String reservationCode
) implements Serializable {
}

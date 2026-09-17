package com.dawn.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationInitRequest {

    private String reservationId;

    @NotNull
    private Long userId;

    @NotNull
    private Long showtimeId;

    @NotNull
    private Long theaterId;

    private String idempotencyKey;
}

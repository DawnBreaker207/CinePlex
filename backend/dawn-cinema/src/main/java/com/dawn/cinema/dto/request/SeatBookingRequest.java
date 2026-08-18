package com.dawn.cinema.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SeatBookingRequest {

    @NotNull
    private Long showtimeId;

    @NotEmpty
    private List<Long> seatIds;

    @NotNull
    private String reservationId;
}
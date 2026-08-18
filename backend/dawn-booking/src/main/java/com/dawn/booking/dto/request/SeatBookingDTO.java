package com.dawn.booking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SeatBookingDTO {

    private Long showtimeId;

    private List<Long> seatIds;

    private String reservationId;
}
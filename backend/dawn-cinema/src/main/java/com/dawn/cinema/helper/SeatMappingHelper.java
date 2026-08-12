package com.dawn.cinema.helper;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.model.SeatInstance;
import com.dawn.cinema.model.SeatTemplate;

import java.math.BigDecimal;

public interface SeatMappingHelper {

    static SeatInstance map(final SeatRequest seat, final SeatTemplate template, final BigDecimal price) {
        return SeatInstance.builder()
                .id(seat.getId())
                .showtimeId(seat.getShowtimeId())
                .seatTemplateId(template.getId())
                .status(seat.getStatus() != null ? seat.getStatus().name() : null)
                .reservationId(seat.getReservationId())
                .price(price)
                .build();
    }

    static SeatResponse map(final SeatInstance seatInstance, final SeatTemplate template) {
        return SeatResponse.builder()
                .id(seatInstance.getId())
                .showtimeId(seatInstance.getShowtimeId())
                .seatNumber(template.getRowLabel() + template.getSeatNumber())
                .build();
    }
}

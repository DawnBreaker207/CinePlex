package com.dawn.booking.helper;

import com.dawn.booking.dto.response.*;
import com.dawn.booking.model.Reservation;
import com.dawn.common.core.constant.ReservationStatus;

import java.util.List;

public interface ReservationMappingHelper {

    static ReservationResponse map(final Reservation reservation, final UserDTO user, final ShowtimeDTO showtime,  final List<SeatDTO> seats) {
        return
                ReservationResponse
                        .builder()
                        .id(reservation.getId())
                        .user(user)
                        .showtime(showtime)
                        .reservationStatus(reservation.getReservationStatus())
                        .totalAmount(reservation.getTotalAmount())
                        .seats(seats
                                .stream()
                                .map(SeatDTO::getSeatNumber)
                                .toList())
                        .isDeleted(reservation.getIsDeleted())
                        .isPaid(ReservationStatus.CONFIRMED.equals(reservation.getReservationStatus()) || ReservationStatus.REFUNDED.equals(reservation.getReservationStatus()))
                        .createdAt(reservation.getCreatedAt())
                        .updatedAt(reservation.getUpdatedAt())
                        .build();

    }

    static UserReservationResponse toUserResponse(
            final Reservation reservation,
            final MovieDTO movie,
            final ShowtimeDTO showtime,
            final List<SeatDTO> seats) {
        return UserReservationResponse.builder()
                .reservationId(reservation.getId())
                .movieTitle(movie.getTitle())
                .moviePoster(movie.getPoster())
                .showtime(showtime.getId())
                .date(showtime.getShowDate())
                .time(showtime.getShowTime())
                .theater(showtime.getTheaterName())
                .seats(seats
                        .stream()
                        .map(SeatDTO::getSeatNumber)
                        .toList())
                .amount(reservation.getTotalAmount().intValue())
                .build();
    }

}

package com.dawn.booking.helper;

import com.dawn.booking.dto.response.*;
import com.dawn.booking.model.Reservation;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.common.core.constant.ReservationStatus;

import java.util.List;

public interface ReservationMappingHelper {

    static ReservationResponse map(final Reservation reservation, final UserResponse user, final ShowtimeResponse showtime,  final List<SeatResponse> seats) {
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
                                .map(SeatResponse::getSeatNumber)
                                .toList())
                        .isDeleted(reservation.getIsDeleted())
                        .isPaid(ReservationStatus.CONFIRMED.equals(reservation.getReservationStatus()) || ReservationStatus.REFUNDED.equals(reservation.getReservationStatus()))
                        .createdAt(reservation.getCreatedAt())
                        .updatedAt(reservation.getUpdatedAt())
                        .build();

    }

    static UserReservationResponse toUserResponse(
            final Reservation reservation,
            final MovieResponse movie,
            final ShowtimeResponse showtime,
            final List<SeatResponse> seats) {
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
                        .map(SeatResponse::getSeatNumber)
                        .toList())
                .amount(reservation.getTotalAmount().intValue())
                .build();
    }

}

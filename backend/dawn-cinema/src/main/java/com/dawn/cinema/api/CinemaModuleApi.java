package com.dawn.cinema.api;

import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;

import java.util.List;

public interface CinemaModuleApi {

    List<ShowtimeResponse> findShowtimesByIds(List<Long> ids);

    ShowtimeResponse findShowtimeById(Long id);

    List<SeatResponse> findSeatsByIds(List<Long> seatIds);

    List<SeatResponse> findSeatsByShowtime(Long showtimeId);

    List<SeatResponse> findSeatsByReservationIds(List<Long> reservationIds);

    List<SeatResponse> findSeatsByReservationId(Long reservationId);

    List<SeatResponse> findSeatsByIdWithLock(List<Long> seatIds);

    int bookSeats(Long showtimeId, List<Long> seatIds, Long reservationId);

    int unbookSeats(Long reservationId, List<Long> seatIds);
}
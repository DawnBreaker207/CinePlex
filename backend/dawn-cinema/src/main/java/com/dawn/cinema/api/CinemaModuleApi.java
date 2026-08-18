package com.dawn.cinema.api;

import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;

import java.util.List;

public interface CinemaModuleApi {

    List<ShowtimeResponse> findShowtimesByIds(List<Long> ids);

    ShowtimeResponse findShowtimeById(Long id);

    List<SeatResponse> findSeatsByIds(List<Long> seatIds);

    List<SeatResponse> findSeatsByShowtime(Long showtimeId);

    List<SeatResponse> findSeatsByReservationIds(List<String> reservationIds);

    List<SeatResponse> findSeatsByReservationId(String reservationId);

    List<SeatResponse> findSeatsByIdWithLock(List<Long> seatIds);

    int bookSeats(Long showtimeId, List<Long> seatIds, String reservationId);

    int unbookSeats(String reservationId, List<Long> seatIds);
}
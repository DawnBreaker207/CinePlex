package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.model.SeatInstance;
import com.dawn.cinema.model.Showtime;

import java.util.List;

public interface SeatService {

    List<SeatResponse> getByShowtime(Long showtimeId);

    List<SeatResponse> getAvailableSeatByShowtime(Long showtimeId);

    List<SeatInstance> create(Showtime showtime);

    List<SeatResponse> findByIdWithLock(List<Long> seatIds);

    List<SeatResponse> findAllById(List<Long> seatIds);

    List<SeatResponse> findAllByShowtimeId(Long showtimeId);

    List<SeatResponse> findAllByReservationIds(List<Long> ids);

    List<SeatResponse> findAllByReservationId(Long reservationId);

    void saveAllSeat(List<SeatRequest> seatRequests);

    int bookSeats(Long showtimeId, List<Long> seatIds, Long reservationId);

    int unbookSeats(Long reservationId, List<Long> seatIds);
}

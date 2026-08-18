package com.dawn.booking.client;

import com.dawn.booking.dto.response.SeatDTO;

import java.util.List;

public interface SeatClientService {

    List<SeatDTO> findByIdWithLock(List<Long> seatIds);

    List<SeatDTO> findAllById(List<Long> seatIds);

    List<SeatDTO> findAllByReservationIds(List<String> reservationIds);

    List<SeatDTO> findAllByReservationId(String reservationId);

    List<SeatDTO> findAllByShowtimeId(Long showtimeId);

    void saveAllSeat(List<SeatDTO> seats);

    int bookSeats(Long showtimeId, List<Long> seatIds, String reservationId);

    int unbookSeats(String reservationId, List<Long> seatIds);
}

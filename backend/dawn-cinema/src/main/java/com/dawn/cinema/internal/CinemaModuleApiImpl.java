package com.dawn.cinema.internal;

import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.service.SeatService;
import com.dawn.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CinemaModuleApiImpl implements CinemaModuleApi {

    private final ShowtimeService showtimeService;

    private final SeatService seatService;

    @Override
    public List<ShowtimeResponse> findShowtimesByIds(List<Long> ids) {
        return showtimeService.findAllByIds(ids);
    }

    @Override
    public ShowtimeResponse findShowtimeById(Long id) {
        return showtimeService.getById(id);
    }

    @Override
    public List<SeatResponse> findSeatsByIds(List<Long> seatIds) {
        return seatService.findAllById(seatIds);
    }

    @Override
    public List<SeatResponse> findSeatsByShowtime(Long showtimeId) {
        return seatService.findAllByShowtimeId(showtimeId);
    }

    @Override
    public List<SeatResponse> findSeatsByReservationIds(List<String> reservationIds) {
        return seatService.findAllByReservationIds(reservationIds);
    }

    @Override
    public List<SeatResponse> findSeatsByReservationId(String reservationId) {
        return seatService.findAllByReservationId(reservationId);
    }

    @Override
    public List<SeatResponse> findSeatsByIdWithLock(List<Long> seatIds) {
        return seatService.findByIdWithLock(seatIds);
    }

    @Override
    public int bookSeats(Long showtimeId, List<Long> seatIds, String reservationId) {
        return seatService.bookSeats(showtimeId, seatIds, reservationId);
    }

    @Override
    public int unbookSeats(String reservationId, List<Long> seatIds) {
        return seatService.unbookSeats(reservationId, seatIds);
    }
}
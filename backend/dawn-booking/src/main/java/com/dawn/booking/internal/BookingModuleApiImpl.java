package com.dawn.booking.internal;

import com.dawn.booking.api.BookingModuleApi;
import com.dawn.booking.dto.response.SseDTO;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingModuleApiImpl implements BookingModuleApi {

    private final CinemaModuleApi cinemaApi;

    private final ReservationRedisService redisService;

    @Override
    public List<SseDTO> findLockedSeatsByShowtime(Long showtimeId) {
        List<Long> allShowtimeSeatIds = cinemaApi
                .findSeatsByShowtime(showtimeId)
                .stream()
                .map(SeatResponse::getId)
                .toList();
        return redisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
    }
}
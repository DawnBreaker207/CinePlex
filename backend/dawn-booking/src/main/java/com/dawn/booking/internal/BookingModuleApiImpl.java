package com.dawn.booking.internal;

import com.dawn.booking.api.BookingModuleApi;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.ReservationDetailResponse;
import com.dawn.booking.dto.response.SseDTO;
import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.ReservationService;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingModuleApiImpl implements BookingModuleApi {

    private final CinemaModuleApi cinemaApi;

    private final ReservationRedisService redisService;

    private final ReservationService reservationService;

    @Override
    public List<SseDTO> findLockedSeatsByShowtime(Long showtimeId) {
        List<Long> allShowtimeSeatIds = cinemaApi
                .findSeatsByShowtime(showtimeId)
                .stream()
                .map(SeatResponse::getId)
                .toList();
        return redisService.getLockedSeatsByShowtime(showtimeId, allShowtimeSeatIds);
    }

    @Override
    public Long getHoldTtlSeconds(String reservationCode) {
        return redisService.getReservationTtl(reservationCode);
    }

    @Override
    public Optional<ReservationDetailResponse> findReservationDetailByCode(String reservationCode) {
        return reservationService.findDetailByCode(reservationCode);
    }

    @Override
    public void cancelReservation(String reservationCode) {
        reservationService.cancel(reservationCode);
    }

    @Override
    public void forceCancelReservation(String reservationCode) {
        reservationService.forceCancel(reservationCode);
    }

    @Override
    public ResponsePage<UserReservationResponse> findReservationHistory(ReservationUserRequest request, Pageable pageable) {
        return reservationService.findByUser(request, pageable);
    }
}

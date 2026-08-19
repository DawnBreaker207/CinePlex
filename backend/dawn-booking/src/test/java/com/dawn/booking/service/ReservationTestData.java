package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.model.Reservation;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.identity.dto.response.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ReservationTestData {

    public static SeatResponse buildSeat(Long id, Long showtimeId) {
        return SeatResponse.builder()
                .id(id)
                .seatNumber("A" + id)
                .status(SeatStatus.AVAILABLE)
                .showtimeId(showtimeId)
                .build();
    }

    public static ShowtimeResponse buildShowtime(Long id) {
        return ShowtimeResponse.builder()
                .id(id)
                .movieId(1L)
                .price(new BigDecimal("100000"))
                .availableSeats(100)
                .showDate(LocalDate.now().plusDays(1))
                .showTime(LocalTime.of(19, 0))
                .build();
    }

    public static UserResponse buildUser(Long id) {
        return UserResponse.builder()
                .userId(id)
                .email("user" + id + "@test.com")
                .build();
    }

    public static Reservation buildReservation(String reservationCode, boolean isPaid) {
        return Reservation.builder()
                .id(1L)
                .reservationCode(reservationCode)
                .userId(1L)
                .showtimeId(10L)
                .reservationStatus(isPaid ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELED)
                .totalAmount(new BigDecimal("100000"))
                .originalAmount(new BigDecimal("100000"))
                .discountAmount(BigDecimal.ZERO)
                .isPaid(isPaid)
                .isDeleted(false)
                .build();
    }

    public static ReservationRedisDTO buildRedisData(String reservationId, List<Long> seatIds) {
        return ReservationRedisDTO.builder()
                .id(reservationId)
                .userId(1L)
                .showtimeId(10L)
                .theaterId(5L)
                .seatsIds(seatIds)
                .build();
    }
}
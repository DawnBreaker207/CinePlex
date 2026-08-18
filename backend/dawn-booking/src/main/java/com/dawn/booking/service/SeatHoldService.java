package com.dawn.booking.service;

import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.response.ReservationInitResponse;

public interface SeatHoldService {

    ReservationInitResponse initReservation(ReservationInitRequest request);

    void holdReservationSeats(ReservationHoldSeatRequest request);

    ReservationInitResponse restoreReservation(String reservationId);
}

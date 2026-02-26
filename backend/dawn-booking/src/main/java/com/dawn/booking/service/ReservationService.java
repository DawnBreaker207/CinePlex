package com.dawn.booking.service;

import com.dawn.booking.dto.request.*;
import com.dawn.booking.dto.response.ReservationInitResponse;
import com.dawn.booking.dto.response.ReservationResponse;
import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

public interface ReservationService {
    ResponsePage<ReservationResponse> findAll(ReservationFilterRequest request, Pageable pageable);

    ResponsePage<UserReservationResponse> findByUser(ReservationUserRequest request, Pageable pageable);

    ReservationResponse findOne(String id);

    ReservationInitResponse restoreReservation(String id);

    ReservationInitResponse initReservation(ReservationInitRequest reservation);

    void holdReservationSeats(ReservationHoldSeatRequest reservation);

    ReservationResponse confirmReservation(String reservationId);

    VoucherDiscountDTO applyVoucher(String reservationId, String code);

    void cancelReservation(String reservationId);

}

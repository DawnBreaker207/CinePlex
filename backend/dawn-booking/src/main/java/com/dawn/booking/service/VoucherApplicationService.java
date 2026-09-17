package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.booking.model.Reservation;

import java.util.Optional;

public interface VoucherApplicationService {

    VoucherDiscountDTO applyVoucher(String reservationId, String code);

    void releaseVoucher(ReservationRedisDTO cachedData, Optional<Reservation> existing);
}

package com.dawn.booking.api;

import com.dawn.booking.dto.response.SseDTO;

import java.util.List;

public interface BookingModuleApi {

    List<SseDTO> findLockedSeatsByShowtime(Long showtimeId);

    // Cross-module read seam: payment guards (createPayment) on the hold state
    // without a cyclic module dependency (dawn-booking never depends on dawn-payment).
    // Remaining hold seconds for a reservation code, null/<=0 means dead hold.
    Long getHoldTtlSeconds(String reservationCode);
}

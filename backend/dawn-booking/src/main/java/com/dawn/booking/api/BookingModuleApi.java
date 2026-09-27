package com.dawn.booking.api;

import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.ReservationDetailResponse;
import com.dawn.booking.dto.response.SseDTO;
import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookingModuleApi {

    List<SseDTO> findLockedSeatsByShowtime(Long showtimeId);

    // Cross-module read seam: payment guards (createPayment) on the hold state
    // without a cyclic module dependency (dawn-booking never depends on dawn-payment).
    // Remaining hold seconds for a reservation code, null/<=0 means dead hold.
    Long getHoldTtlSeconds(String reservationCode);

    Optional<ReservationDetailResponse> findReservationDetailByCode(String reservationCode);

    void cancelReservation(String reservationCode);

    void forceCancelReservation(String reservationCode);

    ResponsePage<UserReservationResponse> findReservationHistory(ReservationUserRequest request, Pageable pageable);
}

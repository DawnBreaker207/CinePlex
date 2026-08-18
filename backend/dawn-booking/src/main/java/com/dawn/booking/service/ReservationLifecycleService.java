package com.dawn.booking.service;

import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.ReservationDetailResponse;
import com.dawn.booking.dto.response.ReservationResponse;
import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReservationLifecycleService {

    ResponsePage<ReservationResponse> findAll(ReservationFilterRequest req, Pageable pageable);

    ReservationResponse findOne(String id);

    Optional<ReservationDetailResponse> findReservationDetail(String id);

    ResponsePage<UserReservationResponse> findByUser(ReservationUserRequest request, Pageable pageable);

    ReservationResponse confirmReservation(String reservationId);

    void cancelReservation(String reservationId);

    void failReservation(String reservationId);

    void expireReservation(String reservationId);

    void forceCancelReservation(String reservationId);
}

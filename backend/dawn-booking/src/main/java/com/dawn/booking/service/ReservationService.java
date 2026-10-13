package com.dawn.booking.service;

import com.dawn.booking.dto.request.CounterSaleRequest;
import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.ReservationDetailResponse;
import com.dawn.booking.dto.response.ReservationResponse;
import com.dawn.booking.dto.response.UserReservationResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.model.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReservationService {

    ResponsePage<ReservationResponse> findAll(ReservationFilterRequest req, Pageable pageable);

    ReservationResponse findByCode(String reservationCode);

    Optional<ReservationDetailResponse> findDetailByCode(String reservationCode);

    ResponsePage<UserReservationResponse> findByUser(ReservationUserRequest request, Pageable pageable);

    ReservationResponse confirm(String reservationId);

    ReservationResponse checkIn(String reservationCode);

    ReservationResponse counterSale(com.dawn.booking.dto.request.CounterSaleRequest request, com.dawn.common.core.model.AuthenticatedUser staff);

    void cancel(String reservationId);

    void fail(String reservationId);

    void expire(String reservationId);

    void forceCancel(String reservationId);
}

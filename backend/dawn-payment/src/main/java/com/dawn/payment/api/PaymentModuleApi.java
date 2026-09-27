package com.dawn.payment.api;

import com.dawn.payment.dto.response.PaymentDetailDTO;

import java.util.Optional;

public interface PaymentModuleApi {

    Optional<PaymentDetailDTO> findPaymentByReservationId(String reservationId);
}

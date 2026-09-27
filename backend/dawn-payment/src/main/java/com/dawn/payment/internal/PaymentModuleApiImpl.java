package com.dawn.payment.internal;

import com.dawn.payment.api.PaymentModuleApi;
import com.dawn.payment.dto.response.PaymentDetailDTO;
import com.dawn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentModuleApiImpl implements PaymentModuleApi {

    private final PaymentService paymentService;

    @Override
    public Optional<PaymentDetailDTO> findPaymentByReservationId(String reservationId) {
        return paymentService.findPaymentByReservationId(reservationId);
    }
}

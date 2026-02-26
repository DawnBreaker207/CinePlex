package com.dawn.payment.dto.request;

import com.dawn.common.core.constant.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUpdateRequest {

    private String reservationId;

    private BigDecimal totalAmount;

    private PaymentMethod method;

    private Boolean isSuccess;
}

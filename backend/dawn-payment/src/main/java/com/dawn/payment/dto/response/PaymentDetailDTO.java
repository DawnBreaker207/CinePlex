package com.dawn.payment.dto.response;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailDTO {
    private PaymentMethod method;

    private PaymentStatus status;

    private String paymentIntentId;
}

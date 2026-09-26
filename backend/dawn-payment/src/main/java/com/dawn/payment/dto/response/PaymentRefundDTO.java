package com.dawn.payment.dto.response;

import com.dawn.common.core.constant.PaymentMethod;
import com.dawn.common.core.constant.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundDTO {

    private String reservationId;

    private BigDecimal amount;

    private PaymentMethod method;

    private PaymentStatus status;

    private String statusReason;

    private Instant paidAt;

    private Instant createdAt;
}
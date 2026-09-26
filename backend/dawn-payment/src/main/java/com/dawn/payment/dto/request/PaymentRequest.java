package com.dawn.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotBlank
    private String reservationId;

    @NotNull
    @Positive
    private Integer amount;

    @NotBlank
    private String paymentType;

    // Snapshot of seats being paid for (C4 reconciliation). Nullable for
    // backward compatibility with clients that only send amount.
    private java.util.List<Long> seatIds;
}

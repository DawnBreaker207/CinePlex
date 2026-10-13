package com.dawn.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for box office in-person counter ticket sales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterSaleRequest {

    @NotNull(message = "Showtime ID is required")
    private Long showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;

    @Builder.Default
    private String paymentMethod = "CASH"; // CASH, CARD_POS, etc.

    private String customerPhone;

    private String customerName;
}

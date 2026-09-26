package com.dawn.booking.dto.request;

import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.request.DatetimeFilterRequest;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ReservationFilterRequest extends DatetimeFilterRequest {
    private String query;

    private String username;

    private Long userId;

    private ReservationStatus reservationStatus;

    private Boolean isPaid;

    private BigDecimal totalAmount;

    private String sortBy = "newest";

    private String sortDirection = "asc";

    @AssertTrue(message = "Sort direction must be 'asc' or 'desc'")
    public boolean isSortDirectionValid() {
        return sortDirection == null || sortDirection.equalsIgnoreCase("asc") || sortDirection.equalsIgnoreCase("desc");
    }

}

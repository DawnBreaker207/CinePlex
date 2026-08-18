package com.dawn.booking.dto.response;

import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.response.BaseResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReservationDetailResponse extends BaseResponse {
    private String id;

    private Long userId;

    private Long showtimeId;

    private ReservationStatus reservationStatus;

    private BigDecimal totalAmount;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private String voucherCode;

    private Boolean isPaid;
}

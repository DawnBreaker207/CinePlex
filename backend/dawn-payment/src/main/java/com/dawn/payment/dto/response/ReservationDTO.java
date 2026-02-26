package com.dawn.payment.dto.response;

import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.response.BaseResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReservationDTO extends BaseResponse {
    private String id;

    private ReservationStatus reservationStatus;

    private BigDecimal totalAmount;

    @Builder.Default
    private List<String> seats = new ArrayList<>();

    private Boolean isDeleted;

    private Boolean isPaid;
}

package com.dawn.booking.dto.response;

import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.response.BaseResponse;
import com.dawn.identity.dto.response.UserResponse;
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
public class ReservationResponse extends BaseResponse {
    private String id;

    private UserResponse user;

    private ShowtimeResponse showtime;

    private ReservationStatus reservationStatus;

    private BigDecimal totalAmount;

    @Builder.Default
    private List<String> seats = new ArrayList<>();

    private Boolean isDeleted;

    private Boolean isPaid;
}

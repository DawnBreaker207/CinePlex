package com.dawn.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReservationRedisDTO {
    private String id;

    private Long userId;

    private Long showtimeId;

    private Long theaterId;

    private String voucherCode;

    @Builder.Default
    private List<Long> seatsIds = new ArrayList<>();
}

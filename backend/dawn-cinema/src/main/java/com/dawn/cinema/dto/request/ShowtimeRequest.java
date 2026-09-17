package com.dawn.cinema.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShowtimeRequest {

    @NotNull
    private Long movieId;

    @NotNull
    private Long theaterId;

    @NotNull
    private LocalDate showDate;

    @NotNull
    private LocalTime showTime;

    @NotNull
    @Positive
    private BigDecimal price;

    private Integer totalSeats;
}

package com.dawn.cinema.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SeatTemplateRequest {

    @NotBlank
    private String rowLabel;

    @NotNull
    @Min(1)
    private Integer seatNumber;

    private String seatType;

    private Integer posX;

    private Integer posY;
}
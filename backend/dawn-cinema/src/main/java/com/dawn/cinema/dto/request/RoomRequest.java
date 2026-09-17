package com.dawn.cinema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RoomRequest {

    @NotBlank
    private String name;

    private String roomType;

    @PositiveOrZero
    private Integer totalSeats;
}

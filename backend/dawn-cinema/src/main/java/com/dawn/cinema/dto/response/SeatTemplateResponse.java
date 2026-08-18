package com.dawn.cinema.dto.response;

import com.dawn.common.core.dto.response.BaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatTemplateResponse extends BaseResponse {

    private Long id;

    private Long roomId;

    private String rowLabel;

    private Integer seatNumber;

    private String seatType;

    private Integer posX;

    private Integer posY;
}
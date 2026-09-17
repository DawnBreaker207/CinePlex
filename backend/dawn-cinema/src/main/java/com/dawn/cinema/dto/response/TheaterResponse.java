package com.dawn.cinema.dto.response;

import com.dawn.common.core.dto.response.BaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheaterResponse extends BaseResponse {
    private Long id;

    private String name;

    private String location;

    private Integer capacity;

    private Boolean isActive;

    @Builder.Default
    private List<Long> showtime = new ArrayList<>();
}

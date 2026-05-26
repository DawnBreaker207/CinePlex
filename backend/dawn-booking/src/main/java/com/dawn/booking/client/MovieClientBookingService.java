package com.dawn.booking.client;


import com.dawn.booking.dto.response.MovieDTO;

import java.util.List;

public interface MovieClientBookingService {
    List<MovieDTO> findAllByIds(List<Long> ids);

    MovieDTO findOne(Long id);
}

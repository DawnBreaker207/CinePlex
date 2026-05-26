package com.dawn.cinema.client;


import com.dawn.cinema.dto.response.MovieDTO;

import java.util.List;

public interface MovieClientCinemaService {
    List<MovieDTO> findAllByIds(List<Long> ids);

    MovieDTO findOne(Long id);
}

package com.dawn.booking.client;


import com.dawn.booking.dto.response.ShowtimeDTO;

import java.util.List;

public interface ShowtimeClientService {
    List<ShowtimeDTO> findAllByIds(List<Long> ids);

    ShowtimeDTO findById(Long id);

    ShowtimeDTO save(ShowtimeDTO showtime);
}

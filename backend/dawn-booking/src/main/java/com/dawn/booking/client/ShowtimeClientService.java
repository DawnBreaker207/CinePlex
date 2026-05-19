package com.dawn.booking.client;


import com.dawn.booking.dto.response.ShowtimeDTO;

public interface ShowtimeClientService {
    ShowtimeDTO findById(Long id);

    ShowtimeDTO save(ShowtimeDTO showtime);
}

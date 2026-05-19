package com.dawn.booking.client;


import com.dawn.booking.dto.response.MovieDTO;

public interface MovieClientBookingService {
    MovieDTO findOne(Long id);
}

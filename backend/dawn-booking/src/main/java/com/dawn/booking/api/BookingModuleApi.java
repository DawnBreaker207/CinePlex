package com.dawn.booking.api;

import com.dawn.booking.dto.response.SseDTO;

import java.util.List;

public interface BookingModuleApi {

    List<SseDTO> findLockedSeatsByShowtime(Long showtimeId);
}
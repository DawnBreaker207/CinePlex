package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.RoomRequest;
import com.dawn.cinema.dto.response.RoomResponse;

import java.util.List;

public interface RoomService {

    List<RoomResponse> findByTheaterId(Long theaterId);

    RoomResponse findOne(Long id);

    RoomResponse create(Long theaterId, RoomRequest request);

    RoomResponse update(Long id, RoomRequest request);

    void remove(Long id);
}

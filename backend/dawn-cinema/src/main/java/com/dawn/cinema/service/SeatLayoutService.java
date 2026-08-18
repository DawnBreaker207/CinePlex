package com.dawn.cinema.service;

import com.dawn.cinema.dto.request.SeatTemplateRequest;
import com.dawn.cinema.dto.response.SeatTemplateResponse;

import java.util.List;

public interface SeatLayoutService {

    List<SeatTemplateResponse> findByRoomId(Long roomId);

    List<SeatTemplateResponse> addSeats(Long roomId, List<SeatTemplateRequest> requests);

    SeatTemplateResponse update(Long roomId, Long templateId, SeatTemplateRequest request);

    void remove(Long roomId, Long templateId);

    List<SeatTemplateResponse> replaceLayout(Long roomId, List<SeatTemplateRequest> requests);
}
package com.dawn.cinema.controller;

import com.dawn.cinema.dto.request.SeatTemplateRequest;
import com.dawn.cinema.dto.response.SeatTemplateResponse;
import com.dawn.cinema.service.SeatLayoutService;
import com.dawn.common.core.dto.response.ResponseObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@Tag(name = "Seat Layout", description = "Operations related to room seat layout")
@RequiredArgsConstructor
@Slf4j
public class SeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    @GetMapping("/{roomId}/seats")
    @Operation(summary = "Get seat layout of a room")
    public ResponseObject<List<SeatTemplateResponse>> findByRoomId(@PathVariable Long roomId) {
        return ResponseObject.success(seatLayoutService.findByRoomId(roomId));
    }

    @PostMapping("/{roomId}/seats")
    @Operation(summary = "Add seats to a room layout (Admin only)")
    public ResponseObject<List<SeatTemplateResponse>> addSeats(@PathVariable Long roomId,
                                                               @Valid @RequestBody List<SeatTemplateRequest> requests) {
        return ResponseObject.created(seatLayoutService.addSeats(roomId, requests));
    }

    @PutMapping("/{roomId}/seats/{templateId}")
    @Operation(summary = "Update a seat in the layout (Admin only)")
    public ResponseObject<SeatTemplateResponse> update(@PathVariable Long roomId,
                                                       @PathVariable Long templateId,
                                                       @Valid @RequestBody SeatTemplateRequest request) {
        return ResponseObject.success(seatLayoutService.update(roomId, templateId, request));
    }

    @DeleteMapping("/{roomId}/seats/{templateId}")
    @Operation(summary = "Remove a seat from the layout (Admin only)")
    public ResponseObject<Void> remove(@PathVariable Long roomId, @PathVariable Long templateId) {
        seatLayoutService.remove(roomId, templateId);
        return ResponseObject.deleted();
    }

    @PutMapping("/{roomId}/layout")
    @Operation(summary = "Replace the whole seat layout of a room (Admin only)")
    public ResponseObject<List<SeatTemplateResponse>> replaceLayout(@PathVariable Long roomId,
                                                                    @Valid @RequestBody List<SeatTemplateRequest> requests) {
        return ResponseObject.success(seatLayoutService.replaceLayout(roomId, requests));
    }
}
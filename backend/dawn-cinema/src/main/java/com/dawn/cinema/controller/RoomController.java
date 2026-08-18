package com.dawn.cinema.controller;

import com.dawn.cinema.dto.request.RoomRequest;
import com.dawn.cinema.dto.response.RoomResponse;
import com.dawn.cinema.service.RoomService;
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
@Tag(name = "Room", description = "Operations related to room")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/theaters/{theaterId}")
    @Operation(summary = "Get all rooms of a theater")
    public ResponseObject<List<RoomResponse>> findByTheaterId(@PathVariable Long theaterId) {
        return ResponseObject.success(roomService.findByTheaterId(theaterId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseObject<RoomResponse> findOne(@PathVariable Long id) {
        return ResponseObject.success(roomService.findOne(id));
    }

    @PostMapping("/theaters/{theaterId}")
    @Operation(summary = "Add a new room to a theater (Admin only)")
    public ResponseObject<RoomResponse> create(@PathVariable Long theaterId, @Valid @RequestBody RoomRequest request) {
        return ResponseObject.created(roomService.create(theaterId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a room (Admin only)")
    public ResponseObject<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseObject.success(roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a room (Admin only)")
    public ResponseObject<Void> remove(@PathVariable Long id) {
        roomService.remove(id);
        return ResponseObject.deleted();
    }
}

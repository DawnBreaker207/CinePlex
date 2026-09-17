package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.RoomRequest;
import com.dawn.cinema.dto.response.RoomResponse;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.repository.TheaterRepository;
import com.dawn.cinema.service.RoomService;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final TheaterRepository theaterRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final com.dawn.common.core.service.AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> findByTheaterId(Long theaterId) {
        return roomRepository.findByTheaterId(theaterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse findOne(Long id) {
        return toResponse(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse create(Long theaterId, RoomRequest request) {
        theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.THEATER_NOT_FOUND.format()));
        assertUniqueName(theaterId, request.getName(), null);
        Room existing = roomRepository.findByTheaterId(theaterId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> r.getName().equalsIgnoreCase(request.getName().trim()))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setName(request.getName().trim());
            existing.setRoomType(request.getRoomType() != null && !request.getRoomType().isBlank()
                    ? request.getRoomType()
                    : existing.getRoomType());
            existing.setTotalSeats(request.getTotalSeats() != null ? request.getTotalSeats() : existing.getTotalSeats());
            existing.setIsActive(true);
            Room reactivated = roomRepository.save(existing);
            auditLogService.record("ROOM_REACTIVATED", "ROOM", reactivated.getId().toString(), null,
                    "INACTIVE", "ACTIVE", "theaterId=" + theaterId + ", name=" + request.getName(),
                    "SUCCESS", AuditLogService.clientIp(), null, null);
            return toResponse(reactivated);
        }
        Room room = Room.builder()
                .theaterId(theaterId)
                .name(request.getName().trim())
                .roomType(request.getRoomType() != null && !request.getRoomType().isBlank()
                        ? request.getRoomType()
                        : "2D")
                .totalSeats(request.getTotalSeats() != null ? request.getTotalSeats() : 0)
                .isActive(true)
                .build();
        return toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findRoom(id);
        if (!room.getName().equalsIgnoreCase(request.getName().trim())) {
            assertUniqueName(room.getTheaterId(), request.getName(), id);
        }
        room.setName(request.getName().trim());
        if (request.getRoomType() != null && !request.getRoomType().isBlank()) {
            room.setRoomType(request.getRoomType());
        }
        if (request.getTotalSeats() != null) {
            room.setTotalSeats(request.getTotalSeats());
        }
        return toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public void remove(Long id) {
        Room room = findRoom(id);
        //  Soft delete: hard delete would cascade to showtimes via FK
        room.setIsActive(false);
        roomRepository.save(room);
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROOM_NOT_FOUND.format()));
    }

    private void assertUniqueName(Long theaterId, String name, Long excludeId) {
        boolean exists = roomRepository.findByTheaterId(theaterId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> excludeId == null || !r.getId().equals(excludeId))
                .anyMatch(r -> r.getName().equalsIgnoreCase(name.trim()));
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ROOM_NAME_EXISTS.format());
        }
    }

    private RoomResponse toResponse(Room room) {
        List<SeatTemplate> templates = seatTemplateRepository.findByRoomId(room.getId());
        int seatCount = (int) templates.stream().filter(t -> Boolean.TRUE.equals(t.getIsActive())).count();
        return RoomResponse.builder()
                .id(room.getId())
                .theaterId(room.getTheaterId())
                .name(room.getName())
                .roomType(room.getRoomType())
                .totalSeats(room.getTotalSeats())
                .seatCount(seatCount)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}

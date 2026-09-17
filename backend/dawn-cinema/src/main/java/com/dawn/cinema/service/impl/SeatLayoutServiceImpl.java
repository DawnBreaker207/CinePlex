package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.SeatTemplateRequest;
import com.dawn.cinema.dto.response.SeatTemplateResponse;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.service.SeatLayoutService;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLayoutServiceImpl implements SeatLayoutService {

    private static final Set<String> SEAT_TYPES = Set.of("NORMAL", "VIP", "COUPLE", "WHEELCHAIR");

    private final RoomRepository roomRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final com.dawn.common.core.service.AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<SeatTemplateResponse> findByRoomId(Long roomId) {
        findRoom(roomId);
        return seatTemplateRepository.findByRoomId(roomId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .sorted(Comparator.comparing(SeatTemplate::getRowLabel)
                        .thenComparing(SeatTemplate::getSeatNumber))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<SeatTemplateResponse> addSeats(Long roomId, List<SeatTemplateRequest> requests) {
        findRoom(roomId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        Set<String> existing = occupiedPositions(roomId);
        Map<String, SeatTemplate> inactiveByKey = findInactiveByKey(roomId);
        List<SeatTemplate> toSave = requests.stream()
                .map(r -> toTemplate(roomId, r, existing, inactiveByKey))
                .toList();
        List<SeatTemplate> saved = seatTemplateRepository.saveAll(toSave);
        auditReactivated(roomId, saved);
        syncTotalSeats(roomId);
        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SeatTemplateResponse update(Long roomId, Long templateId, SeatTemplateRequest request) {
        findRoom(roomId);
        SeatTemplate template = findTemplate(templateId);
        String newKey = key(request.getRowLabel().trim(), request.getSeatNumber());
        boolean positionChanged = !template.getRowLabel().equalsIgnoreCase(request.getRowLabel().trim())
                || !template.getSeatNumber().equals(request.getSeatNumber());
        if (positionChanged) {
            String currentKey = key(template.getRowLabel(), template.getSeatNumber());
            Set<String> occupied = occupiedPositions(roomId);
            occupied.remove(currentKey);
            if (!occupied.add(newKey)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        ErrorCode.SEAT_TEMPLATE_DUPLICATE.format(request.getRowLabel(), request.getSeatNumber()));
            }
        }
        template.setRowLabel(request.getRowLabel().trim());
        template.setSeatNumber(request.getSeatNumber());
        template.setSeatType(normalizeSeatType(request.getSeatType()));
        template.setPosX(request.getPosX() != null ? request.getPosX() : 0);
        template.setPosY(request.getPosY() != null ? request.getPosY() : 0);
        return toResponse(seatTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public void remove(Long roomId, Long templateId) {
        findRoom(roomId);
        SeatTemplate template = findTemplate(templateId);
        //  Soft delete: seat_instance rows keep their seatTemplateId reference
        template.setIsActive(false);
        seatTemplateRepository.save(template);
        syncTotalSeats(roomId);
    }

    @Override
    @Transactional
    public List<SeatTemplateResponse> replaceLayout(Long roomId, List<SeatTemplateRequest> requests) {
        findRoom(roomId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        seatTemplateRepository.findByRoomId(roomId).forEach(t -> t.setIsActive(false));
        Map<String, SeatTemplate> inactiveByKey = findInactiveByKey(roomId);
        List<SeatTemplate> toSave = requests.stream()
                .map(r -> toTemplate(roomId, r, null, inactiveByKey))
                .toList();
        List<SeatTemplate> saved = seatTemplateRepository.saveAll(toSave);
        auditReactivated(roomId, saved);
        syncTotalSeats(roomId);
        return saved.stream().map(this::toResponse).toList();
    }

    private Map<String, SeatTemplate> findInactiveByKey(Long roomId) {
        Map<String, SeatTemplate> byKey = new HashMap<>();
        seatTemplateRepository.findByRoomId(roomId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsActive()))
                .forEach(t -> byKey.put(key(t.getRowLabel(), t.getSeatNumber()), t));
        return byKey;
    }

    private void auditReactivated(Long roomId, List<SeatTemplate> saved) {
        long reactivated = saved.stream().filter(t -> t.getId() != null).count();
        if (reactivated > 0) {
            auditLogService.record("SEAT_TEMPLATE_REACTIVATED", "SEAT_TEMPLATE", String.valueOf(roomId), null,
                    "INACTIVE", "ACTIVE", "roomId=" + roomId + ", count=" + reactivated,
                    "SUCCESS", AuditLogService.clientIp(), null, null);
        }
    }

    private SeatTemplate toTemplate(Long roomId, SeatTemplateRequest request, Set<String> existing,
            Map<String, SeatTemplate> inactiveByKey) {
        if (existing != null) {
            String key = key(request.getRowLabel().trim(), request.getSeatNumber());
            if (!existing.add(key)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        ErrorCode.SEAT_TEMPLATE_DUPLICATE.format(request.getRowLabel(), request.getSeatNumber()));
            }
        }
        SeatTemplate reactivated = inactiveByKey != null
                ? inactiveByKey.remove(key(request.getRowLabel().trim(), request.getSeatNumber()))
                : null;
        if (reactivated != null) {
            reactivated.setSeatType(normalizeSeatType(request.getSeatType()));
            reactivated.setPosX(request.getPosX() != null ? request.getPosX() : 0);
            reactivated.setPosY(request.getPosY() != null ? request.getPosY() : 0);
            reactivated.setIsActive(true);
            return reactivated;
        }
        return SeatTemplate.builder()
                .roomId(roomId)
                .rowLabel(request.getRowLabel().trim())
                .seatNumber(request.getSeatNumber())
                .seatType(normalizeSeatType(request.getSeatType()))
                .posX(request.getPosX() != null ? request.getPosX() : 0)
                .posY(request.getPosY() != null ? request.getPosY() : 0)
                .isActive(true)
                .build();
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROOM_NOT_FOUND.format()));
    }

    private SeatTemplate findTemplate(Long templateId) {
        return seatTemplateRepository.findById(templateId)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SEAT_TEMPLATE_NOT_FOUND.format()));
    }

    private Set<String> occupiedPositions(Long roomId) {
        Set<String> keys = new HashSet<>();
        seatTemplateRepository.findByRoomId(roomId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .forEach(t -> keys.add(key(t.getRowLabel(), t.getSeatNumber())));
        return keys;
    }

    private String normalizeSeatType(String seatType) {
        if (seatType == null || seatType.isBlank()) {
            return "NORMAL";
        }
        String normalized = seatType.toUpperCase();
        if (!SEAT_TYPES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.SEAT_TYPE_INVALID.format(seatType));
        }
        return normalized;
    }

    private String key(String rowLabel, Integer seatNumber) {
        return rowLabel.trim().toUpperCase(java.util.Locale.ROOT) + "-" + seatNumber;
    }

    private void syncTotalSeats(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return;
        }
        long count = seatTemplateRepository.findByRoomId(roomId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .count();
        room.setTotalSeats((int) count);
        roomRepository.save(room);
    }

    private SeatTemplateResponse toResponse(SeatTemplate template) {
        return SeatTemplateResponse.builder()
                .id(template.getId())
                .roomId(template.getRoomId())
                .rowLabel(template.getRowLabel())
                .seatNumber(template.getSeatNumber())
                .seatType(template.getSeatType())
                .posX(template.getPosX())
                .posY(template.getPosY())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
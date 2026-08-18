package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.helper.SeatMappingHelper;
import com.dawn.cinema.model.Room;
import com.dawn.cinema.model.SeatInstance;
import com.dawn.cinema.model.SeatTemplate;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.repository.RoomRepository;
import com.dawn.cinema.repository.SeatInstanceRepository;
import com.dawn.cinema.repository.SeatTemplateRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.service.SeatService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {

    private final SeatInstanceRepository seatInstanceRepository;

    private final SeatTemplateRepository seatTemplateRepository;

    private final RoomRepository roomRepository;

    private final ShowtimeRepository showtimeRepository;

    @Override
    public List<SeatResponse> getByShowtime(Long showtimeId) {
        log.info("Fetching seats for showtime id: {}", showtimeId);
        Showtime showtime = showtimeRepository
                .findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND));

        List<SeatInstance> seats = seatInstanceRepository.findAllByShowtimeId(showtimeId);

        if (seats.isEmpty()) {
            log.warn("No seats found for showtime id: {}. Creating seats automatically.", showtimeId);
            try {
                seats = create(showtime);
                log.info("Successfully created {} seats for showtime id: {}", seats.size(), showtimeId);
            } catch (Exception e) {
                log.error("Failed to create seats for showtime id: {}, Error: {}", showtimeId, e.getMessage(), e);
                throw new InternalServiceException("Failed to create seats for showtime: " + e.getMessage());
            }
        }

        Map<Long, SeatTemplate> templateMap = getSeatTemplateMap(showtime.getRoomId());
        return seats.stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public List<SeatResponse> getAvailableSeatByShowtime(Long showtimeId) {
        log.info("Fetching available seats for showtime id: {}", showtimeId);

        Showtime showtime = showtimeRepository
                .findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));

        List<SeatInstance> allSeats = seatInstanceRepository.findAllByShowtimeId(showtimeId);
        if (allSeats.isEmpty()) {
            log.warn("No seats found for showtime id: {}. Creating seats before fetching available ones.", showtimeId);
            try {
                create(showtime);
            } catch (Exception e) {
                log.error("Failed to create seats for showtime id: {}, Error: {}", showtimeId, e.getMessage(), e);
                throw new InternalServiceException("Failed to create seats for showtime: " + e.getMessage());
            }
        }

        Map<Long, SeatTemplate> templateMap = getSeatTemplateMap(showtime.getRoomId());
        return seatInstanceRepository
                .findByShowtimeIdAndStatus(showtimeId, SeatStatus.AVAILABLE.name())
                .stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByReservationIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        log.info("Batch fetching {} seats", ids.size());
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllByReservationIdIn(ids);
        if (seatInstances.isEmpty()) return List.of();
        Long showtimeId = seatInstances.getFirst().getShowtimeId();
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        Map<Long, SeatTemplate> templateMap = showtime != null
                ? getSeatTemplateMap(showtime.getRoomId())
                : Map.of();
        return seatInstances.stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> findByIdWithLock(List<Long> seatIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findByIdWithLock(seatIds);
        if (seatInstances.isEmpty()) return List.of();
        Long showtimeId = seatInstances.getFirst().getShowtimeId();
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        Map<Long, SeatTemplate> templateMap = showtime != null
                ? getSeatTemplateMap(showtime.getRoomId())
                : Map.of();
        return seatInstances.stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public List<SeatResponse> findAllById(List<Long> seatIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(seatIds);
        if (seatInstances.isEmpty()) return List.of();
        Long showtimeId = seatInstances.getFirst().getShowtimeId();
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        Map<Long, SeatTemplate> templateMap = showtime != null
                ? getSeatTemplateMap(showtime.getRoomId())
                : Map.of();
        return seatInstances.stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByShowtimeId(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        Map<Long, SeatTemplate> templateMap = showtime != null
                ? getSeatTemplateMap(showtime.getRoomId())
                : Map.of();
        return seatInstanceRepository
                .findAllByShowtimeId(showtimeId)
                .stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByReservationId(String reservationId) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllByReservationId(reservationId);
        if (seatInstances.isEmpty()) return List.of();
        Long showtimeId = seatInstances.getFirst().getShowtimeId();
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        Map<Long, SeatTemplate> templateMap = showtime != null
                ? getSeatTemplateMap(showtime.getRoomId())
                : Map.of();
        return seatInstances.stream()
                .map(s -> toResponse(s, templateMap.get(s.getSeatTemplateId())))
                .toList();
    }

    @Override
    public void saveAllSeat(List<SeatRequest> seatRequests) {
        if (seatRequests == null || seatRequests.isEmpty()) {
            return;
        }
        List<Long> seatIds = seatRequests.stream().map(SeatRequest::getId).toList();
        List<SeatInstance> existingSeats = seatInstanceRepository.findAllById(seatIds);
        Map<Long, SeatRequest> requestMap = seatRequests.stream().collect(Collectors.toMap(SeatRequest::getId, Function.identity()));

        for (SeatInstance seat : existingSeats) {
            SeatRequest request = requestMap.get(seat.getId());
            if (request.getStatus() != null) {
                seat.setStatus(request.getStatus().name());
            }
            seat.setReservationId(request.getReservationId());
        }

        seatInstanceRepository.saveAll(existingSeats);
    }

    @Override
    @Transactional
    public int bookSeats(Long showtimeId, List<Long> seatIds, String reservationId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE);
        }
        int booked = seatInstanceRepository.bookSeats(showtimeId, seatIds, SeatStatus.BOOKED.name(), reservationId);
        if (booked != seatIds.size()) {
            log.warn("CAS book failed: expected {} seats, got {} for showtime {}", seatIds.size(), booked, showtimeId);
            throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE);
        }
        return booked;
    }

    @Override
    @Transactional
    public int unbookSeats(String reservationId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }
        return seatInstanceRepository.unbookSeats(reservationId, seatIds);
    }

    @Override
    public List<SeatInstance> create(Showtime showtime) {
        if (showtime == null) {
            throw new IllegalArgumentException("Showtime cannot be null");
        }

        Long roomId = showtime.getRoomId();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        List<SeatTemplate> templates = seatTemplateRepository.findByRoomId(roomId);
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("No seat templates found for room: " + roomId);
        }

        log.info("Creating {} seat instances for showtime id: {} in room id: {}", templates.size(), showtime.getId(), roomId);

        List<SeatInstance> seats = new ArrayList<>();
        for (SeatTemplate template : templates) {
            SeatInstance seat = SeatInstance.builder()
                    .showtimeId(showtime.getId())
                    .seatTemplateId(template.getId())
                    .status(SeatStatus.AVAILABLE.name())
                    .price("VIP".equals(template.getSeatType()) && showtime.getVipPrice() != null
                            ? showtime.getVipPrice()
                            : showtime.getPrice())
                    .build();
            seats.add(seat);
        }

        return seatInstanceRepository.saveAll(seats);
    }

    private Map<Long, SeatTemplate> getSeatTemplateMap(Long roomId) {
        return seatTemplateRepository.findByRoomId(roomId)
                .stream()
                .collect(Collectors.toMap(SeatTemplate::getId, Function.identity()));
    }

    private SeatResponse toResponse(SeatInstance seatInstance, SeatTemplate template) {
        if (template == null) {
            return SeatResponse.builder()
                    .id(seatInstance.getId())
                    .showtimeId(seatInstance.getShowtimeId())
                    .build();
        }
        return SeatResponse.builder()
                .id(seatInstance.getId())
                .showtimeId(seatInstance.getShowtimeId())
                .seatNumber(template.getRowLabel() + template.getSeatNumber())
                .build();
    }
}

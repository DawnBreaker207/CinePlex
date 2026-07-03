package com.dawn.cinema.service.impl;

import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.helper.SeatMappingHelper;
import com.dawn.cinema.model.Seat;
import com.dawn.cinema.model.Showtime;
import com.dawn.cinema.repository.SeatRepository;
import com.dawn.cinema.repository.ShowtimeRepository;
import com.dawn.cinema.service.SeatService;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
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

    private static final Long THEATER_ID_STANDARD = 1L;
    private static final Long THEATER_ID_VIP = 2L;

    private final SeatRepository seatRepository;

    private final ShowtimeRepository showtimeRepository;

    @Override
    public List<SeatResponse> getByShowtime(Long showtimeId) {
        log.info("Fetching seats for showtime id: {}", showtimeId);
        Showtime showtime = showtimeRepository
                .findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.SHOWTIME_NOT_FOUND));

        log.info("Found showtime: {} for movie: {} at theater: {}",
                showtimeId,
                showtime.getMovieId() != null ? showtime.getMovieId() : "unknown",
                showtime.getTheater() != null ? showtime.getTheater().getName() : "unknown");

        List<Seat> seats = seatRepository.findByShowtime(showtime);

        if (seats.isEmpty()) {
            log.warn("No seats found for showtime id: {}. Creating seats automatically.", showtimeId);
            try {
                seats = create(showtime);
                log.info("Successfully created {} seats for showtime id: {}", seats.size(), showtime);
            } catch (Exception e) {
                log.error("Failed to create seats for showtime id: {}, Error: {}", showtimeId, e.getMessage(), e);
                throw new InternalServiceException("Failed to create seats for showtime: " + e.getMessage());
            }
        } else {
            log.info("Found {} existing seats for showtime id: {}", seats.size(), showtimeId);
        }

        return seats.stream().map(SeatMappingHelper::map).toList();

    }

    @Override
    public List<SeatResponse> getAvailableSeatByShowtime(Long showtimeId) {
        log.info("Fetching available seats for showtime id: {}", showtimeId);

        Showtime showtime = showtimeRepository
                .findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.THEATER_NOT_FOUND));

        List<Seat> allSeats = seatRepository.findByShowtime(showtime);
        if (allSeats.isEmpty()) {
            log.warn("No seats found for showtime id: {}. Creating seats before fetching available ones.", showtimeId);
            try {
                create(showtime);
            } catch (Exception e) {
                log.error("Failed to create seats for showtime id: {}, Error: {}", showtimeId, e.getMessage(), e);
                throw new InternalServiceException("Failed to create seats for showtime: " + e.getMessage());
            }
        }

        return seatRepository
                .findByShowtimeAndStatus(showtime, SeatStatus.AVAILABLE)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByReservationIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        log.info("Batch fetching {} seats", ids.size());
        return seatRepository.findAllByReservationIdIn(ids)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> findByIdWithLock(List<Long> seatIds) {
        return seatRepository
                .findByIdWithLock(seatIds)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    public List<SeatResponse> findAllById(List<Long> seatIds) {
        return seatRepository
                .findAllById(seatIds)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByShowtimeId(Long showtimeId) {
        return seatRepository
                .findAllByShowtimeId(showtimeId)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    public List<SeatResponse> findAllByReservationId(String reservationId) {
        return seatRepository
                .findAllByReservationId(reservationId)
                .stream()
                .map(SeatMappingHelper::map)
                .toList();
    }

    @Override
    public void saveAllSeat(List<SeatRequest> seatRequests) {
        if (seatRequests == null || seatRequests.isEmpty()) {
            return;
        }
        List<Long> seatIds = seatRequests.stream().map(SeatRequest::getId).toList();
        List<Seat> existingSeats = seatRepository.findAllById(seatIds);
        Map<Long, SeatRequest> requestMap = seatRequests.stream().collect(Collectors.toMap(SeatRequest::getId, Function.identity()));

        for (Seat seat : existingSeats) {
            SeatRequest request = requestMap.get(seat.getId());
            seat.setStatus(request.getStatus());
            seat.setReservationId(request.getReservationId());
        }

        seatRepository.saveAll(existingSeats);
    }

    @Override
    public List<Seat> create(Showtime showtime) {
        if (showtime == null) {
            throw new IllegalArgumentException("Showtime cannot be null");
        }

        if (showtime.getTheater() == null) {
            throw new IllegalArgumentException("Showtime must have an associated theater");
        }

        Long theaterId = showtime.getTheater().getId();
        Integer capacity = showtime.getTheater().getCapacity();

        log.info("Creating seats for showtime id: {} in theater id: {} with capacity: {}", showtime.getId(), theaterId, capacity);

        List<Seat> seats = new ArrayList<>();

        if (THEATER_ID_STANDARD.equals(theaterId)) {
            log.info("Creating seats for theater layout (15 rows x 20 seats)");
            for (char row = 'A'; row <= 'O'; row++) {
                for (int seatNum = 1; seatNum <= 10; seatNum++) {
                    Seat seat = new Seat();
                    seat.setShowtime(showtime);
                    seat.setSeatNumber(row + String.valueOf(seatNum));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seats.add(seat);
                }
            }
        } else if (THEATER_ID_VIP.equals(theaterId)) {
            log.info("Creating seats for theater layout (10 rows x 20 seats)");
            for (char row = 'A'; row <= 'J'; row++) {
                for (char seatNum = 'A'; seatNum <= '0'; seatNum++) {
                    Seat seat = new Seat();
                    seat.setShowtime(showtime);
                    seat.setSeatNumber(row + String.valueOf(seatNum));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seats.add(seat);
                }
            }
        } else {
            int rows = (int) Math.ceil(Math.sqrt(capacity));
            int seatsPerRow = (int) Math.ceil((double) capacity / rows);
            log.info("Creating seats for theater id: {} with dynamic layout ({} rows x {} seats)", theaterId, rows, seatsPerRow);

            for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
                char row = (char) ('A' + rowIdx);
                for (int seatNum = 1; seatNum <= seatsPerRow && seats.size() < capacity; seatNum++) {
                    Seat seat = new Seat();
                    seat.setShowtime(showtime);
                    seat.setSeatNumber(row + String.valueOf(seatNum));
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seats.add(seat);
                }
            }
        }

        if (seats.size() != showtime.getTotalSeats()) {
            log.warn("Created {} seats but showtime has {} total seats. Updating showtime.", seats.size(), showtime.getTotalSeats());
            showtime.setTotalSeats(seats.size());
            showtime.setAvailableSeats(seats.size());
            showtimeRepository.save(showtime);
        }
        log.info("Created {} seats for showtime id: {}, saving to database...", seats.size(), showtime.getId());
        return seatRepository.saveAll(seats);
    }
}

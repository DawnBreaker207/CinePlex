package com.dawn.cinema.controller;

import com.dawn.cinema.dto.request.SeatBookingRequest;
import com.dawn.cinema.dto.request.SeatRequest;
import com.dawn.cinema.dto.request.SeatUnbookingRequest;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.service.SeatService;
import com.dawn.common.core.dto.response.ResponseObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@Tag(name = "Seats", description = "Operations related to seat")
@RequiredArgsConstructor
@Slf4j
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/showtime/{showtimeId}")
    @Operation(summary = "Get all seats for a showtime", description = "Returns all seats for a specific showtime")
    public ResponseObject<List<SeatResponse>> getByShowtime(@PathVariable Long showtimeId) {
        log.info("Fetching seats for showtime id: {}", showtimeId);
        return ResponseObject.success(seatService.getByShowtime(showtimeId));
    }

    @GetMapping("/showtime/{showtimeId}/available")
    @Operation(summary = "Get available seats for a showtime", description = "Returns available seats for a specific showtime")
    public ResponseObject<List<SeatResponse>> getAvailableSeatsByShowtime(@PathVariable Long showtimeId) {
        log.info("Fetching available seats for showtime id: {}", showtimeId);
        return ResponseObject.success(seatService.getAvailableSeatByShowtime(showtimeId));
    }

    @PostMapping("/reservation/batch-by-reservations")
    public ResponseObject<List<SeatResponse>> findAllByReservationIds(@RequestBody List<String> ids) {
        return ResponseObject.success(seatService.findAllByReservationIds(ids));
    }

    @PostMapping("/showtime/{showtimeId}/create")
    @Operation(summary = "Create seat for a showtime", description = "Manually create seats for a specific showtime (Admin only)")
    public ResponseObject<List<SeatResponse>> createSeatsForShowtime(@PathVariable Long showtimeId) {
        log.info("Manually creating seats for showtime id: {}", showtimeId);
        return ResponseObject.success(seatService.getAvailableSeatByShowtime(showtimeId));
    }

    @PostMapping("/locks")
    public ResponseObject<List<SeatResponse>> findSeatByIdWithLock(@RequestBody List<Long> seatIds) {
        return ResponseObject.success(seatService.findByIdWithLock(seatIds));
    }

    @PostMapping("/all/id")
    public ResponseObject<List<SeatResponse>> findAllSeatById(@RequestBody List<Long> seatIds) {
        return ResponseObject.success(seatService.findAllById(seatIds));
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseObject<List<SeatResponse>> findAllSeatByReservationId(@PathVariable String reservationId) {
        return ResponseObject.success(seatService.findAllByReservationId(reservationId));
    }

    @GetMapping("/reservation/showtime/{showtimeId}")
    public ResponseObject<List<SeatResponse>> findAllSeatByShowtimeId(@PathVariable Long showtimeId) {
        return ResponseObject.success(seatService.findAllByShowtimeId(showtimeId));
    }


    @PostMapping("/saveAll")
    public ResponseObject<Void> saveAllSeat(@RequestBody @Valid List<SeatRequest> seats) {
        seatService.saveAllSeat(seats);
        return ResponseObject.deleted();
    }

    @PostMapping("/book")
    public ResponseObject<Integer> bookSeats(@RequestBody @Valid SeatBookingRequest request) {
        return ResponseObject.success(seatService.bookSeats(request.getShowtimeId(), request.getSeatIds(), request.getReservationId()));
    }

    @PostMapping("/unbook")
    public ResponseObject<Integer> unbookSeats(@RequestBody @Valid SeatUnbookingRequest request) {
        return ResponseObject.success(seatService.unbookSeats(request.getReservationId(), request.getSeatIds()));
    }
}

package com.dawn.booking.controller;

import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.*;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.ReservationService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservation")
@Tag(name = "Reservation", description = "Operations related to reservation")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationController {

    ReservationService reservationService;

    ReservationRedisService redisService;

    @GetMapping("")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(summary = "Get all reservation with conditions", description = "Returns reservation with condition filters (Admin Only)")
    public ResponseObject<ResponsePage<ReservationResponse>> getAll(@ModelAttribute ReservationFilterRequest o, Pageable pageable) {
        return ResponseObject.success(reservationService.findAll(o, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by id", description = "Returns reservation by its Id (Admin Only)")
    public ResponseObject<ReservationResponse> getOne(@PathVariable String id) {
        return ResponseObject.success(reservationService.findOne(id));
    }

    @GetMapping("/me")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get reservation by user id", description = "Returns reservation by they own Id (User Only)")
    public ResponseObject<ResponsePage<UserReservationResponse>> getAllByUser(@ModelAttribute ReservationUserRequest request, Pageable pageable) {
        return ResponseObject.success(reservationService.findByUser(request, pageable));
    }

    @PostMapping("/{reservationId}/voucher")
    @Operation(summary = "Apply voucher", description = "Apply a voucher code to the current reservation session")
    public ResponseObject<VoucherDiscountDTO> applyVoucher(@PathVariable String reservationId, @RequestParam String code) {
        return ResponseObject.success(reservationService.applyVoucher(reservationId, code));
    }


    @GetMapping("/{reservationId}/restore")
    @Operation(summary = "Restore a reservation", description = "Restore a reservation and return data")
    public ResponseObject<ReservationInitResponse> restoreReservation(@PathVariable String reservationId) {
        return ResponseObject.success(reservationService.restoreReservation(reservationId));
    }

    @PostMapping("/init")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Init a reservation", description = "Create a reservation and return Id")
    public ResponseObject<ReservationInitResponse> reservationInit(@RequestBody ReservationInitRequest reservation) {
        return ResponseObject.success(reservationService.initReservation(reservation));
    }

    @PostMapping("/seatHold")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Choose and booking seat", description = "Selected seat place and booking it")
    public ResponseObject<Void> reservationHoldSeat(@RequestBody ReservationHoldSeatRequest o) {
        reservationService.holdReservationSeats(o);
        return ResponseObject.success(null);
    }

    @PostMapping("/confirm/{reservationId}")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Save reservation after payment ", description = "Returns reservation after booking seats and payment success")
    public ResponseObject<ReservationResponse> reservationConfirm(@PathVariable String reservationId) {
        return ResponseObject.created(reservationService.confirmReservation(reservationId));
    }

    @PostMapping("/{reservationId}/cancel")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel reservation after payment ", description = "Cancel reservation after booking seats and payment failed")
    public ResponseObject<Void> reservationCancel(@PathVariable String reservationId) {
        reservationService.cancelReservation(reservationId);
        return ResponseObject.success(null);
    }

    @GetMapping("/showtimes/{showtimeId}/locked-seats")
    public List<SseDTO> getLockedSeats(@PathVariable Long showtimeId) {
        return redisService.getLockedSeatsByShowtime(showtimeId);
    }
}

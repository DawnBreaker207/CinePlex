package com.dawn.booking.service.impl;

import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.*;
import com.dawn.booking.helper.ReservationMappingHelper;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.ReservationLifecycleService;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.ReservationStateMachine;
import com.dawn.booking.service.VoucherApplicationService;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.aspect.AuditLogContext;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationLifecycleServiceImpl implements ReservationLifecycleService {

    ReservationRepository reservationRepository;

    CinemaModuleApi cinemaApi;

    IdentityModuleApi identityApi;

    CatalogModuleApi catalogApi;

    ReservationNotificationHelper reservationNotificationHelper;

    ReservationRedisService reservationRedisService;

    VoucherApplicationService voucherApplicationService;

    @Override
    public ResponsePage<UserReservationResponse> findByUser(ReservationUserRequest request, Pageable pageable) {
        log.debug("Finding reservation for user {}, status={}", request.getUserId(), request.getStatus());

        Page<Reservation> reservations = reservationRepository
                .findAllByUserIdAndReservationStatusOrderByCreatedAtDesc(
                        request.getUserId(),
                        ReservationStatus.CONFIRMED,
                        pageable);
        if (reservations.isEmpty()) {
            return ResponsePage.of(reservations.map(r -> null));
        }
        log.info("Found {} reservations for user {}", reservations.getSize(), request.getUserId());

        List<Long> showtimeIds = reservations.stream()
                .map(Reservation::getShowtimeId)
                .distinct()
                .toList();

        Map<Long, ShowtimeResponse> showtimeMap = cinemaApi.findShowtimesByIds(showtimeIds)
                .stream()
                .collect(Collectors.toMap(ShowtimeResponse::getId, s -> s));

        List<Long> movieIds = showtimeMap.values().stream()
                .map(ShowtimeResponse::getMovieId)
                .distinct()
                .toList();

        Map<Long, MovieResponse> movieMap = catalogApi.findMoviesByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieResponse::getId, s -> s));

        List<String> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        Map<String, List<SeatResponse>> seatMap = cinemaApi.findSeatsByReservationIds(reservationIds)
                .stream()
                .filter(item -> item.getReservationId() != null)
                .collect(Collectors.groupingBy(SeatResponse::getReservationId));

        return ResponsePage.of(reservations
                .map(reservation -> {
                    ShowtimeResponse showtime = showtimeMap.get(reservation.getShowtimeId());
                    MovieResponse movie = movieMap.get(showtime.getMovieId());
                    List<SeatResponse> seats = seatMap.getOrDefault(reservation.getId(), List.of());
                    return ReservationMappingHelper.toUserResponse(reservation, movie, showtime, seats);
                }));
    }

    @Override
    public ResponsePage<ReservationResponse> findAll(ReservationFilterRequest req, Pageable pageable) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

        //  Convert to Instant
        Instant startDate = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endDate = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Page<Reservation> reservations = reservationRepository
                .findAllWithFilter(req, startDate, endDate, pageable);
        if (reservations.isEmpty()) {
            return ResponsePage.of(reservations.map(r -> null));
        }

        List<Long> showtimeIds = reservations.stream()
                .map(Reservation::getShowtimeId)
                .distinct()
                .toList();

        Map<Long, ShowtimeResponse> showtimeMap = cinemaApi.findShowtimesByIds(showtimeIds)
                .stream()
                .collect(Collectors.toMap(ShowtimeResponse::getId, s -> s));

        List<Long> userIds = reservations.stream()
                .map(Reservation::getUserId)
                .distinct()
                .toList();

        Map<Long, UserResponse> userMap = identityApi.findUsersByIds(userIds)
                .stream()
                .collect(Collectors.toMap(UserResponse::getUserId, s -> s));

        List<String> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        Map<String, List<SeatResponse>> seatMap = cinemaApi.findSeatsByReservationIds(reservationIds)
                .stream()
                .filter(s -> s.getReservationId() != null)
                .collect(Collectors.groupingBy(SeatResponse::getReservationId));

        return ResponsePage.of(reservations.map(reservation -> {
            ShowtimeResponse showtime = showtimeMap.get(reservation.getShowtimeId());
            UserResponse user = userMap.get(reservation.getUserId());
            List<SeatResponse> seats = seatMap.getOrDefault(reservation.getId(), List.of());
            return ReservationMappingHelper.map(reservation, user, showtime, seats);
        }));
    }

    @Override
    public ReservationResponse findOne(String id) {
        Reservation reservation = reservationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND.format()));

        List<SeatResponse> seats = cinemaApi.findSeatsByReservationId(reservation.getId());
        ShowtimeResponse showtime = cinemaApi.findShowtimeById(reservation.getShowtimeId());
        UserResponse user = identityApi.findUserById(reservation.getUserId());
        return ReservationMappingHelper.map(reservation, user, showtime, seats);
    }

    @Override
    public Optional<ReservationDetailResponse> findReservationDetail(String id) {
        return reservationRepository.findById(id)
                .map(r -> ReservationDetailResponse.builder()
                        .id(r.getId())
                        .userId(r.getUserId())
                        .showtimeId(r.getShowtimeId())
                        .reservationStatus(r.getReservationStatus())
                        .totalAmount(r.getTotalAmount())
                        .originalAmount(r.getOriginalAmount())
                        .discountAmount(r.getDiscountAmount())
                        .voucherCode(r.getVoucherCode())
                        .isPaid(ReservationStatus.CONFIRMED.equals(r.getReservationStatus())
                                || ReservationStatus.REFUNDED.equals(r.getReservationStatus()))
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build());
    }

    @Override
    @Transactional
    @AuditLog(action = "'RESERVATION_CONFIRMED'", entity = "'RESERVATION'", entityId = "#reservationId",
            fromState = "'PENDING'", toState = "'CONFIRMED'",
            metadata = "'showtimeId=' + #audit['showtimeId'] + ', seats=' + #audit['seats'] + ', total=' + #audit['total']")
    public ReservationResponse confirmReservation(String reservationId) {

        // Idempotency + state guard
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            ReservationStatus status = existing.get().getReservationStatus();
            if (status == ReservationStatus.CONFIRMED) {
                log.info("Reservation {} already confirmed, returning existing", reservationId);
                Reservation r = existing.get();
                List<SeatResponse> seats = cinemaApi.findSeatsByReservationId(reservationId);
                ShowtimeResponse showtime = cinemaApi.findShowtimeById(r.getShowtimeId());
                UserResponse user = identityApi.findUserById(r.getUserId());
                return ReservationMappingHelper.map(r, user, showtime, seats);
            }
            if (!ReservationStateMachine.canTransition(status, ReservationStateMachine.Action.CONFIRM)) {
                log.warn("Reservation {} is in state {}, cannot confirm", reservationId, status);
                throw new ApiException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_INVALID_STATUS.format());
            }
        }

        //  Processing lock guards the DB write; short TTL (A3)
        if (!reservationRedisService.tryAcquireProcessingLock(reservationId)) {
            log.warn("Reservation {} is already being processed", reservationId);
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_PROCESSING.format());
        }

        try {
            //  Collect data from redis
            ReservationRedisDTO cachedData = reservationRedisService.getFromRedis(reservationId);
            if (cachedData == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.format());
            }
            log.info("Get reservation from redis: {}", cachedData);


            List<Long> seatIds = cachedData.getSeatsIds();
            if (seatIds == null || seatIds.isEmpty()) {
                throw new IllegalStateException(ErrorCode.NO_SEAT_SELECTED.format());
            }
            //  Validate seat lock
            reservationRedisService.validateSeatLocks(reservationId, seatIds);
            UserResponse user = identityApi.findUserById(cachedData.getUserId());
            log.info("Get user from reservation {}", user);
            List<SeatResponse> seatEntities = loadSeatFromDatabase(seatIds, reservationId);
            //        Create reservation to save
            ShowtimeResponse showtime = cinemaApi.findShowtimeById(cachedData.getShowtimeId());

            //  CAS book seats in DB (atomic, no load-modify-save race)
            int booked = cinemaApi.bookSeats(showtime.getId(), seatIds, reservationId);
            if (booked != seatIds.size()) {
                log.warn("CAS book failed for reservation {}: booked {}/{} seats", reservationId, booked, seatIds.size());
                throw new SeatUnavailableException(ErrorCode.SEAT_UNAVAILABLE.format());
            }

            //  Calculate voucher, not used
            log.info("All {} seats verified as available in DB for reservation {}", seatEntities.size(), reservationId);
            String voucherCode = cachedData.getVoucherCode();
            BigDecimal originalAmount = showtime.getPrice().multiply(BigDecimal.valueOf(seatEntities.size()));
            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal total = originalAmount;
            if (voucherCode != null && !voucherCode.isBlank()) {
                VoucherCalculation finalCalc = catalogApi.calculateVoucher(voucherCode, originalAmount);
                discountAmount = finalCalc.getDiscountAmount();
                total = finalCalc.getFinalAmount();
            }
            log.info("Calculated total amount: {} for {} seats", total, seatEntities.size());


            // Commit reseration
            Reservation reservation = Reservation
                    .builder()
                    .id(reservationId)
                    .userId(user.getUserId())
                    .showtimeId(showtime.getId())
                    .reservationStatus(ReservationStatus.CONFIRMED)
                    .originalAmount(originalAmount)
                    .discountAmount(discountAmount)
                    .totalAmount(total)
                    .voucherCode(voucherCode)
                    .isPaid(true)
                    .isDeleted(false)
                    .build();

            Reservation savedReservation;
            try {
                savedReservation = reservationRepository.saveAndFlush(reservation);
            } catch (Exception e) {
                // Seats were booked remotely; compensate before rethrowing
                log.error("Failed to save reservation {}, compensating seat booking", reservationId, e);
                try {
                    cinemaApi.unbookSeats(reservationId, seatIds);
                } catch (Exception ex) {
                    log.error("Compensation failed for reservation {}: {}", reservationId, ex.getMessage());
                }
                throw e;
            }

            // Update side effect
            try {
                if (voucherCode != null && !voucherCode.isBlank()) {
                    catalogApi.useVoucher(voucherCode, cachedData.getUserId(), reservationId);
                }
            } catch (Exception e) {
                log.warn("Failed to use voucher {} for reservation {}", voucherCode, reservationId);
            }

            try {
                reservationNotificationHelper.handleNotification(savedReservation, showtime, seatEntities);
            } catch (Exception e) {
                log.warn("Failed to send notification for reservation {}", reservationId);

            }

            reservationRedisService.cleanupRedisLocks(reservationId, seatEntities);
            AuditLogContext.set("showtimeId", showtime.getId());
            AuditLogContext.set("seats", seatIds);
            AuditLogContext.set("total", total);
            log.info("Successfully confirmed reservation: {} with {} seats", reservation.getId(), seatEntities.size());
            return ReservationMappingHelper.map(savedReservation, user, showtime, seatEntities);
        } finally {
            reservationRedisService.releaseProcessingLock(reservationId);
        }
    }

    @Override
    @AuditLog(action = "'RESERVATION_CANCELED'", entity = "'RESERVATION'", entityId = "#reservationId",
            fromState = "#audit['fromState']", toState = "'CANCELED'",
            metadata = "'seats=' + #audit['seats']")
    public void cancelReservation(String reservationId) {
        //  User-initiated cancel on a paid reservation must be explicit, not a silent no-op
        reservationRepository.findById(reservationId)
                .filter(r -> r.getReservationStatus() == ReservationStatus.CONFIRMED)
                .ifPresent(r -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_INVALID_STATUS.format());
                });
        releaseReservation(reservationId, ReservationStateMachine.Action.CANCEL);
    }

    @Override
    @AuditLog(action = "'RESERVATION_FAILED'", entity = "'RESERVATION'", entityId = "#reservationId",
            fromState = "#audit['fromState']", toState = "'FAILED'",
            metadata = "'seats=' + #audit['seats']")
    public void failReservation(String reservationId) {
        releaseReservation(reservationId, ReservationStateMachine.Action.FAIL);
    }

    @Override
    @AuditLog(action = "'RESERVATION_EXPIRED'", entity = "'RESERVATION'", entityId = "#reservationId",
            fromState = "#audit['fromState']", toState = "'EXPIRED'",
            metadata = "'seats=' + #audit['seats']")
    public void expireReservation(String reservationId) {
        releaseReservation(reservationId, ReservationStateMachine.Action.EXPIRE);
    }

    private void releaseReservation(String reservationId, ReservationStateMachine.Action action) {
        ReservationStatus newStatus = action.targetStatus();
        log.info("Releasing reservation {} as {}", reservationId, newStatus);

        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()
                && !ReservationStateMachine.canTransition(existing.get().getReservationStatus(), action)) {
            log.warn("Reservation {} already in final state {}, skipping release", reservationId,
                    existing.get().getReservationStatus());
            return;
        }

        AuditLogContext.set("fromState", existing.map(r -> r.getReservationStatus().name()).orElse(null));

        //        Get reservation id from redis (may have expired already)
        ReservationRedisDTO cachedData = null;
        try {
            cachedData = reservationRedisService.getFromRedis(reservationId);
        } catch (Exception e) {
            log.warn("Reservation {} not in Redis anymore, releasing from DB only", reservationId);
        }

        List<Long> seatIds = cachedData != null ? cachedData.getSeatsIds() : List.of();
        if (cachedData != null) {
            if (seatIds != null && !seatIds.isEmpty()) {
                reservationRedisService.deleteSeatLocks(seatIds, reservationId);
            }
            reservationRedisService.deleteReservation(reservationId);
        }

        //  Defensive CAS release of DB seats (idempotent)
        if (seatIds != null && !seatIds.isEmpty()) {
            try {
                cinemaApi.unbookSeats(reservationId, seatIds);
            } catch (Exception e) {
                log.warn("Failed to unbook seats for reservation {}: {}", reservationId, e.getMessage());
            }
        }

        //   Save record with new status
        if (existing.isEmpty()) {
            if (cachedData == null) {
                log.warn("No DB row and no Redis data for reservation {}, nothing to release", reservationId);
                return;
            }
            int seatCount = seatIds != null ? seatIds.size() : 0;
            BigDecimal price = new BigDecimal(cachedData.getPrice());
            BigDecimal total = price.multiply(BigDecimal.valueOf(seatCount));
            log.info("Calculated total amount: {} for {} seats", total, seatCount);
            Reservation reservation = Reservation
                    .builder()
                    .id(reservationId)
                    .userId(cachedData.getUserId())
                    .showtimeId(cachedData.getShowtimeId())
                    .reservationStatus(newStatus)
                    .originalAmount(total)
                    .totalAmount(total)
                    .voucherCode(cachedData.getVoucherCode())
                    .isDeleted(false)
                    .build();
            reservationRepository.save(reservation);
            log.info("Saved {} reservation: {}", newStatus, reservationId);
        } else {
            Reservation r = existing.get();
            r.setReservationStatus(newStatus);
            reservationRepository.save(r);
            log.info("Updated reservation {} to {}", reservationId, newStatus);
        }

        AuditLogContext.set("seats", seatIds);

        try {
            if (cachedData != null) {
                List<Long> allShowtimeSeatIds = cinemaApi
                        .findSeatsByShowtime(cachedData.getShowtimeId())
                        .stream()
                        .map(SeatResponse::getId)
                        .toList();
                reservationNotificationHelper.getSeatRelease(cachedData.getShowtimeId(), cachedData.getUserId(), allShowtimeSeatIds);
                log.info("Published seat release event for showtime {}: {}", cachedData.getShowtimeId(), seatIds);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast seat release for reservation {}", reservationId);
        }

        try {
            voucherApplicationService.releaseVoucher(cachedData, existing);
        } catch (Exception e) {
            log.warn("Failed to release voucher for reservation {}", reservationId);
        }
    }

    @Override
    @AuditLog(action = "'RESERVATION_CANCELED'", entity = "'RESERVATION'", entityId = "#reservationId",
            fromState = "#audit['fromState']", toState = "'CANCELED'", metadata = "'force cancel'")
    public void forceCancelReservation(String reservationId) {
        log.info("Force cancel reservation: {}", reservationId);
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        String fromStatus = existing.map(r -> r.getReservationStatus().name()).orElse(null);
        AuditLogContext.set("fromState", fromStatus);
        ReservationRedisDTO cachedData = null;
        try {
            cachedData = reservationRedisService.getFromRedis(reservationId);
        } catch (Exception e) {
            log.warn("Reservation {} not in Redis anymore, force-canceling from DB only", reservationId);
        }
        if (existing.isPresent()) {
            Reservation r = existing.get();
            r.setReservationStatus(ReservationStatus.CANCELED);
            r.setIsDeleted(true);
            reservationRepository.save(r);
        }
        try {
            if (cachedData != null) {
                List<Long> seatIds = cachedData.getSeatsIds();
                if (seatIds != null && !seatIds.isEmpty()) {
                    reservationRedisService.deleteSeatLocks(seatIds, reservationId);
                    cinemaApi.unbookSeats(reservationId, seatIds);
                }
                reservationRedisService.deleteReservation(reservationId);
            }
        } catch (Exception e) {
            log.warn("Redis cleanup failed for force-canceled reservation {}", reservationId);
        }

        try {
            Long showtimeId = cachedData != null ? cachedData.getShowtimeId()
                    : existing.map(Reservation::getShowtimeId).orElse(null);
            Long userId = cachedData != null ? cachedData.getUserId()
                    : existing.map(Reservation::getUserId).orElse(null);
            if (showtimeId != null && userId != null) {
                List<Long> allShowtimeSeatIds = cinemaApi.findSeatsByShowtime(showtimeId)
                        .stream()
                        .map(SeatResponse::getId)
                        .toList();
                reservationNotificationHelper.getSeatRelease(showtimeId, userId, allShowtimeSeatIds);
                log.info("Published seat release event for force-canceled reservation {}", reservationId);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast seat release for reservation {}", reservationId);
        }

        try {
            voucherApplicationService.releaseVoucher(cachedData, existing);
        } catch (Exception e) {
            log.warn("Failed to release voucher for reservation {}", reservationId);
        }
    }

    //    Reservation private method
    private List<SeatResponse> loadSeatFromDatabase(List<Long> seatIds, String reservationId) {
        //        Take seat from request
        List<SeatResponse> seats = cinemaApi.findSeatsByIdWithLock(seatIds);
        if (seats.size() != seatIds.size()) {
            log.error("Expected {} seats but found {} for reservation {}", seatIds.size(), seats.size(), reservationId);
            throw new IllegalStateException(ErrorCode.RESERVATION_SEATS_NOT_FOUND_DB.format());
        }

        return seats;
    }
}

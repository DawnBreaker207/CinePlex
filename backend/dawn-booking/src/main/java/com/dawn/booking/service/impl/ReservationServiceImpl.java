package com.dawn.booking.service.impl;

import com.dawn.booking.client.SeatClientService;
import com.dawn.booking.client.ShowtimeClientService;
import com.dawn.booking.client.UserClientService;
import com.dawn.booking.dto.request.ReservationFilterRequest;
import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.request.ReservationUserRequest;
import com.dawn.booking.dto.response.*;
import com.dawn.booking.helper.ReservationMappingHelper;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.ReservationService;
import com.dawn.booking.utils.ReservationUtils;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.common.core.service.AuditLogService;
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
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationServiceImpl implements ReservationService {

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(Constants.RESERVATION_HOLD_MINUTES);

    ReservationRepository reservationRepository;

    SeatClientService seatService;

    UserClientService userService;

    ShowtimeClientService showtimeService;

    CatalogModuleApi catalogApi;

    ReservationNotificationHelper reservationNotificationHelper;

    ReservationRedisService reservationRedisService;

    AuditLogService auditLogService;

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

        Map<Long, ShowtimeDTO> showtimeMap = showtimeService.findAllByIds(showtimeIds)
                .stream()
                .collect(Collectors.toMap(ShowtimeDTO::getId, s -> s));

        List<Long> movieIds = showtimeMap.values().stream()
                .map(ShowtimeDTO::getMovieId)
                .distinct()
                .toList();

        Map<Long, MovieResponse> movieMap = catalogApi.findMoviesByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieResponse::getId, s -> s));

        List<String> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        Map<String, List<SeatDTO>> seatMap = seatService.findAllByReservationIds(reservationIds)
                .stream()
                .filter(item -> item.getReservationId() != null)
                .collect(Collectors.groupingBy(SeatDTO::getReservationId));

        return ResponsePage.of(reservations
                .map(reservation -> {
                    ShowtimeDTO showtime = showtimeMap.get(reservation.getShowtimeId());
                    MovieResponse movie = movieMap.get(showtime.getMovieId());
                    List<SeatDTO> seats = seatMap.getOrDefault(reservation.getId(), List.of());
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

        Map<Long, ShowtimeDTO> showtimeMap = showtimeService.findAllByIds(showtimeIds)
                .stream()
                .collect(Collectors.toMap(ShowtimeDTO::getId, s -> s));

        List<Long> userIds = reservations.stream()
                .map(Reservation::getUserId)
                .distinct()
                .toList();

        Map<Long, UserDTO> userMap = userService.findAllByIds(userIds)
                .stream()
                .collect(Collectors.toMap(UserDTO::getUserId, s -> s));

        List<String> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        Map<String, List<SeatDTO>> seatMap = seatService.findAllByReservationIds(reservationIds)
                .stream()
                .filter(s -> s.getReservationId() != null)
                .collect(Collectors.groupingBy(SeatDTO::getReservationId));

        return ResponsePage.of(reservations.map(reservation -> {
            ShowtimeDTO showtime = showtimeMap.get(reservation.getShowtimeId());
            UserDTO user = userMap.get(reservation.getUserId());
            List<SeatDTO> seats = seatMap.getOrDefault(reservation.getId(), List.of());
            return ReservationMappingHelper.map(reservation, user, showtime, seats);
        }));
    }

    @Override
    public ReservationResponse findOne(String id) {
        Reservation reservation = reservationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND));

        List<SeatDTO> seats = seatService.findAllByReservationId(reservation.getId());
        ShowtimeDTO showtime = showtimeService.findById(reservation.getShowtimeId());
        UserDTO user = userService.findById(reservation.getUserId());
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
    public ReservationInitResponse restoreReservation(String reservationId) {
        log.info("Restore reservation with id: {}", reservationId);
        Long ttl = reservationRedisService.getReservationTtl(reservationId);

        if (ttl == null || ttl <= 0) {
            log.warn("Reservation {} not found or expired in Redis", reservationId);
            throw new ResourceNotFoundException(Message.Exception.RESERVATION_EXPIRED);
        }

        Map<Object, Object> reservation = reservationRedisService.getReservationData(reservationId);

        if (reservation == null || reservation.isEmpty()) {
            throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
        }

        String showtimeIdStr = (String) reservation.get(Constants.REDIS_SHOWTIME_ID);
        Instant expiredAt = Instant.now().plusSeconds(ttl);

        return ReservationInitResponse.builder()
                .reservationId(reservationId)
                .showtimeId(Long.valueOf(showtimeIdStr))
                .ttl(ttl)
                .expiredAt(expiredAt)
                .build();
    }

    @Override
    public ReservationInitResponse initReservation(ReservationInitRequest o) {
        log.info("Initializing reservation for user {} at showtime {}", o.getUserId(), o.getShowtimeId());

        String reservationId = ReservationUtils.generateReservationId();
        ShowtimeDTO showtime = showtimeService.findById(o.getShowtimeId());
        //        Create essential value to save on redis
        Map<String, String> initialData = Map.of(
                Constants.REDIS_RESERVATION_ID, reservationId,
                Constants.REDIS_USER_ID, o.getUserId().toString(),
                Constants.REDIS_SHOWTIME_ID, o.getShowtimeId().toString(),
                Constants.REDIS_THEATER_ID, o.getTheaterId().toString(),
                Constants.REDIS_PRICE, showtime.getPrice().toPlainString(),
                Constants.REDIS_SEAT_IDS, "[]");

        //        Create expired time on redis key
        reservationRedisService.saveReservationInit(reservationId, initialData, HOLD_TIMEOUT);

        log.info("Reservation initialize, Id: {}, user Id: {}, showtime Id: {}, theater Id: {} , ttl: {}",
                reservationId,
                o.getUserId(),
                o.getShowtimeId(),
                o.getTheaterId(),
                HOLD_TIMEOUT.toSeconds());

        return ReservationInitResponse.builder()
                .reservationId(reservationId)
                .showtimeId(o.getShowtimeId())
                .ttl(HOLD_TIMEOUT.toSeconds())
                .expiredAt(Instant.now().plusSeconds(HOLD_TIMEOUT.toSeconds()))
                .build();
    }

    public void holdReservationSeats(ReservationHoldSeatRequest reservation) {
        Long userId = reservation.getUserId();
        String reservationId = reservation.getReservationId();
        Long showtimeId = reservation.getShowtimeId();
        List<Long> seatIds = reservation.getSeatIds();
        log.info("Holding {} seats for reservation {} (user:{}, showtime: {})", seatIds.size(), reservationId, userId, showtimeId);

        //        Take reservationId and userId on redis
        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);

        Map<Object, Object> reservationData = reservationRedisService.getReservationData(reservationId);
        log.info("Get reservation redis from hold seat {}", reservationData);
        validateReservationOwnership(reservationData, reservationId, userId);
        validateShowtimeAndAvailability(showtimeId, seatIds.size());


        List<Long> allShowtimeSeatIds = seatService.findAllByShowtimeId(showtimeId)
                .stream()
                .map(SeatDTO::getId)
                .toList();
        // Delete old seats in Redis
        List<Long> oldSeatIds = reservationRedisService.parseSeatIdsFromReservationData(reservationData);
        List<Long> seatRelease = oldSeatIds
                .stream()
                .filter(id -> !seatIds.contains(id))
                .toList();
        if (!seatRelease.isEmpty()) {
            reservationRedisService.deleteSeatLocks(seatRelease, reservationId);
            reservationNotificationHelper.sendSeatRelease(showtimeId, seatRelease, allShowtimeSeatIds);
            log.info("Released seats {} for reservation {}", seatRelease, reservationId);
        }

        //        Load seats from DB and validate
        List<SeatDTO> seats = seatService.findAllById(seatIds);
        log.info("Get seat from DB: {}", seats.size());
        validateSeatsForReservation(seats, showtimeId, seatIds);

        //        Lock seats from Redis
        reservationRedisService.acquireSeatLock(seatIds, seats, redisKey);

        try {
            //        Update seat in redis
            reservationRedisService.updateReservationSeats(reservationId, seatIds);

            //        Upsert PENDING reservation row so ExpirationJob can track the hold
            reservationRepository.save(Reservation.builder()
                    .id(reservationId)
                    .userId(userId)
                    .showtimeId(showtimeId)
                    .reservationStatus(ReservationStatus.PENDING)
                    .expiredAt(Instant.now().plus(HOLD_TIMEOUT))
                    .isDeleted(false)
                    .build());

            reservationNotificationHelper.sendSeatHold(showtimeId, userId, allShowtimeSeatIds);
            auditLogService.record("RESERVATION_HOLD", "RESERVATION", reservationId, null, "PENDING",
                    "showtimeId=" + showtimeId + ", seats=" + seatIds);
            log.info("Successfully hold {} seats with user id {} for reservation {}: {} ", seatIds.size(), userId, reservationId, seatIds);

        } catch (Exception e) {
            log.error("Error after locking seats, processing unlocking for reservation: {}", reservationId);
            reservationRedisService.deleteSeatLocks(seatIds, reservationId);
            throw e;
        }
    }

    @Override
    @Transactional
    public ReservationResponse confirmReservation(String reservationId) {

        // Idempotency + state guard
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            ReservationStatus status = existing.get().getReservationStatus();
            if (status == ReservationStatus.CONFIRMED) {
                log.info("Reservation {} already confirmed, returning existing", reservationId);
                Reservation r = existing.get();
                List<SeatDTO> seats = seatService.findAllByReservationId(reservationId);
                ShowtimeDTO showtime = showtimeService.findById(r.getShowtimeId());
                UserDTO user = userService.findById(r.getUserId());
                return ReservationMappingHelper.map(r, user, showtime, seats);
            }
            if (status != ReservationStatus.PENDING) {
                log.warn("Reservation {} is in state {}, cannot confirm", reservationId, status);
                throw new ApiException(HttpStatus.CONFLICT, Message.Exception.RESERVATION_INVALID_STATUS);
            }
        }

        //  Processing lock guards the DB write; short TTL (A3)
        if (!reservationRedisService.tryAcquireProcessingLock(reservationId)) {
            log.warn("Reservation {} is already being processed", reservationId);
            throw new ApiException(HttpStatus.CONFLICT, Message.Exception.RESERVATION_PROCESSING);
        }

        try {
            //  Collect data from redis
            ReservationRedisDTO cachedData = reservationRedisService.getFromRedis(reservationId);
            if (cachedData == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, Message.Exception.RESERVATION_NOT_FOUND);
            }
            log.info("Get reservation from redis: {}", cachedData);


            List<Long> seatIds = cachedData.getSeatsIds();
            if (seatIds == null || seatIds.isEmpty()) {
                throw new IllegalStateException(Message.Exception.NO_SEAT_SELECTED);
            }
            //  Validate seat lock
            reservationRedisService.validateSeatLocks(reservationId, seatIds);
            UserDTO user = userService.findById(cachedData.getUserId());
            log.info("Get user from reservation {}", user);
            List<SeatDTO> seatEntities = loadSeatFromDatabase(seatIds, reservationId);
            //        Create reservation to save
            ShowtimeDTO showtime = showtimeService.findById(cachedData.getShowtimeId());

            //  CAS book seats in DB (atomic, no load-modify-save race)
            int booked = seatService.bookSeats(showtime.getId(), seatIds, reservationId);
            if (booked != seatIds.size()) {
                log.warn("CAS book failed for reservation {}: booked {}/{} seats", reservationId, booked, seatIds.size());
                throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE);
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
                    seatService.unbookSeats(reservationId, seatIds);
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
            auditLogService.record("RESERVATION_CONFIRMED", "RESERVATION", reservationId, "PENDING", "CONFIRMED",
                    "showtimeId=" + showtime.getId() + ", seats=" + seatIds + ", total=" + total);
            log.info("Successfully confirmed reservation: {} with {} seats", reservation.getId(), seatEntities.size());
            return ReservationMappingHelper.map(savedReservation, user, showtime, seatEntities);
        } finally {
            reservationRedisService.releaseProcessingLock(reservationId);
        }
    }

    @Override
    public VoucherDiscountDTO applyVoucher(String reservationId, String code) {
        ReservationRedisDTO redisData = reservationRedisService.getFromRedis(reservationId);

        ShowtimeDTO showtime = showtimeService.findById(redisData.getShowtimeId());
        BigDecimal seatTotal = showtime.getPrice().multiply(BigDecimal.valueOf(redisData.getSeatsIds().size()));

        VoucherCalculation calc = catalogApi.calculateVoucher(code, seatTotal);
        VoucherDiscountDTO discount = VoucherDiscountDTO.builder()
                .code(calc.getCode())
                .originalAmount(calc.getOriginalAmount())
                .discountAmount(calc.getDiscountAmount())
                .finalAmount(calc.getFinalAmount())
                .build();

        redisData.setVoucherCode(code);
        reservationRedisService.saveVoucher(redisData);

        return discount;
    }

    @Override
    public void cancelReservation(String reservationId) {
        releaseReservation(reservationId, ReservationStatus.CANCELED);
    }

    @Override
    public void failReservation(String reservationId) {
        releaseReservation(reservationId, ReservationStatus.FAILED);
    }

    @Override
    public void expireReservation(String reservationId) {
        releaseReservation(reservationId, ReservationStatus.EXPIRED);
    }

    private void releaseReservation(String reservationId, ReservationStatus newStatus) {
        log.info("Releasing reservation {} as {}", reservationId, newStatus);

        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            ReservationStatus current = existing.get().getReservationStatus();
            if (current == ReservationStatus.CONFIRMED
                    || current == ReservationStatus.CANCELED
                    || current == ReservationStatus.FAILED
                    || current == ReservationStatus.EXPIRED) {
                log.warn("Reservation {} already in final state {}, skipping release", reservationId, current);
                return;
            }
        }

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
                seatService.unbookSeats(reservationId, seatIds);
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

        auditLogService.record("RESERVATION_" + newStatus.name(), "RESERVATION", reservationId, null, newStatus.name(),
                "seats=" + seatIds);

        try {
            if (cachedData != null) {
                List<Long> allShowtimeSeatIds = seatService
                        .findAllByShowtimeId(cachedData.getShowtimeId())
                        .stream()
                        .map(SeatDTO::getId)
                        .toList();
                reservationNotificationHelper.getSeatRelease(cachedData.getShowtimeId(), cachedData.getUserId(), allShowtimeSeatIds);
                log.info("Published seat release event for showtime {}: {}", cachedData.getShowtimeId(), seatIds);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast seat release for reservation {}", reservationId);
        }

        String voucherCode = cachedData != null ? cachedData.getVoucherCode()
                : existing.map(Reservation::getVoucherCode).orElse(null);
        Long voucherUserId = cachedData != null ? cachedData.getUserId()
                : existing.map(Reservation::getUserId).orElse(null);
        if (voucherCode != null && !voucherCode.isBlank() && voucherUserId != null) {
            try {
                catalogApi.releaseVoucher(voucherCode, voucherUserId);
            } catch (Exception e) {
                log.warn("Failed to release voucher {} for reservation {}", voucherCode, reservationId);
            }
        }
    }

    @Override
    public void forceCancelReservation(String reservationId) {
        log.info("Force cancel reservation: {}", reservationId);
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent()) {
            Reservation r = existing.get();
            r.setReservationStatus(ReservationStatus.CANCELED);
            r.setIsDeleted(true);
            reservationRepository.save(r);
        }
        try {
            ReservationRedisDTO cachedData = reservationRedisService.getFromRedis(reservationId);
            if (cachedData != null) {
                List<Long> seatIds = cachedData.getSeatsIds();
                if (seatIds != null && !seatIds.isEmpty()) {
                    reservationRedisService.deleteSeatLocks(seatIds, reservationId);
                    seatService.unbookSeats(reservationId, seatIds);
                }
                reservationRedisService.deleteReservation(reservationId);
            }
        } catch (Exception e) {
            log.warn("Redis cleanup failed for force-canceled reservation {}", reservationId);
        }
    }

    private void validateSeatsForReservation(List<SeatDTO> seats, Long showtimeId, List<Long> seatIds) {
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats
                    .stream()
                    .map(SeatDTO::getId)
                    .toList();
            List<Long> notFoundSeatIds = seatIds
                    .stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .toList();
            throw new SeatUnavailableException(Message.Exception.SEAT_NOT_FOUND + notFoundSeatIds);
        }
        //        Verify seats belong to the request showtime
        List<String> wrongShowtimeSeats = new ArrayList<>();
        for (SeatDTO seat : seats) {
            if (!seat.getShowtimeId().equals(showtimeId)) {
                wrongShowtimeSeats.add(seat.getSeatNumber());
            }
        }

        if (!wrongShowtimeSeats.isEmpty()) {
            String wrongSeatNumbers = String.join(", ", wrongShowtimeSeats);
            throw new SeatUnavailableException(Message.format(Message.Exception.SEAT_WRONG_SHOWTIME, wrongSeatNumbers));
        }

        //        Check seat in showtime was booked in database
        List<String> bookedSeats = new ArrayList<>();
        for (SeatDTO seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                bookedSeats.add(seat.getSeatNumber());
            }
        }
        if (!bookedSeats.isEmpty()) {
            String bookedSeatNumbers = String.join(", ", bookedSeats);
            throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE + " " + bookedSeatNumbers);
        }

    }

    private void validateReservationOwnership(Map<Object, Object> reservationData, String reservationId, Long userId) {
        log.info("Validate owner ship");
        if (reservationData == null || reservationData.isEmpty()) {
            throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
        }
        String userIdStr = (String) reservationData.get(Constants.REDIS_USER_ID);
        log.info("User id {}", userIdStr);
        if (!userIdStr.equals(String.valueOf(userId))) {
            throw new PermissionDeniedException(Message.Exception.PERMISSION_FORBIDDEN);
        }
        //        Check and compare value valid
        String reservationIdStr = (String) reservationData.get(Constants.REDIS_RESERVATION_ID);
        log.info("Reservation id: {}", reservationIdStr);
        if (reservationIdStr == null || !reservationIdStr.equals(reservationId)) {
            throw new ResourceNotFoundException(Message.Exception.RESERVATION_INVALID_DATA);
        }
        //        Check user existed
        log.info("User service find by id");
        userService.findById(userId);
    }

    private void validateShowtimeAndAvailability(Long showtimeId, int requestSeats) {
        //      Validate showtime
        ShowtimeDTO showtime = showtimeService.findById(showtimeId);
        log.info("Find showtime {}", showtime);
        //      Check showtime is in the past
        if (showtime.getShowDate().isBefore((LocalDate.now())) ||
                (showtime.getShowDate().isEqual(LocalDate.now()) &&
                        showtime.getShowTime().isBefore(LocalTime.now()))) {
            throw new IllegalStateException(Message.Exception.RESERVATION_PAST_SHOWTIME);
        }

        //        Check if showtime has enough available seats
        if (showtime.getAvailableSeats() < requestSeats) {
            throw new IllegalStateException(Message.format(Message.Exception.RESERVATION_NOT_ENOUGH_SEATS, requestSeats, showtime.getAvailableSeats()));
        }
    }

    //    Reservation private method
    private List<SeatDTO> loadSeatFromDatabase(List<Long> seatIds, String reservationId) {
        //        Take seat from request
        List<SeatDTO> seats = seatService.findByIdWithLock(seatIds);
        if (seats.size() != seatIds.size()) {
            log.error("Expected {} seats but found {} for reservation {}", seatIds.size(), seats.size(), reservationId);
            throw new IllegalStateException(Message.Exception.RESERVATION_SEATS_NOT_FOUND_DB);
        }

        return seats;
    }


}

package com.dawn.booking.service.impl;

import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.response.ReservationInitResponse;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.SeatHoldService;
import com.dawn.booking.utils.ReservationUtils;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.common.infra.redis.service.VelocityGuard;
import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.exception.ApiException;
import com.dawn.identity.api.IdentityModuleApi;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SeatHoldServiceImpl implements SeatHoldService {

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(Constants.RESERVATION_HOLD_MINUTES);

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 3;

    ReservationRepository reservationRepository;

    CinemaModuleApi cinemaApi;

    IdentityModuleApi identityApi;

    ReservationNotificationHelper reservationNotificationHelper;

    ReservationRedisService reservationRedisService;

    VelocityGuard velocityGuard;

    @Override
    public ReservationInitResponse resumeReservation(String reservationId) {
        log.info("Restore reservation with id: {}", reservationId);
        Long ttl = reservationRedisService.getReservationTtl(reservationId);

        if (ttl == null || ttl <= 0) {
            log.warn("Reservation {} not found or expired in Redis", reservationId);
            throw new ResourceNotFoundException(ErrorCode.RESERVATION_EXPIRED.format());
        }

        Map<Object, Object> reservation = reservationRedisService.getReservationData(reservationId);

        if (reservation == null || reservation.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND.format());
        }

        String showtimeIdStr = (String) reservation.get(Constants.REDIS_SHOWTIME_ID);
        Instant expiredAt = Instant.now().plusSeconds(ttl);

        return ReservationInitResponse.builder()
                .reservationCode(reservationId)
                .showtimeId(Long.valueOf(showtimeIdStr))
                .ttl(ttl)
                .expiredAt(expiredAt)
                .build();
    }

    @Override
    public ReservationInitResponse initReservation(ReservationInitRequest o) {
        log.info("Initializing reservation for user {} at showtime {}", o.getUserId(), o.getShowtimeId());

        String idempotencyKey = o.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ReservationInitResponse existing = findExistingByIdempotencyKey(idempotencyKey, o);
            if (existing != null) {
                log.info("Idempotency key {} already used, returning existing reservation {}", idempotencyKey, existing.getReservationCode());
                return existing;
            }
        }

        if (o.getUserId() != null && !velocityGuard.tryAcquire(
                "hold:init:" + o.getUserId(), Constants.HOLD_MAX_INITS,
                Duration.ofMinutes(Constants.HOLD_INIT_WINDOW_MINUTES))) {
            log.warn("Hold velocity exceeded for user {}", o.getUserId());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.HOLD_TOO_MANY_ATTEMPTS.format());
        }

        String reservationId = ReservationUtils.generateReservationCode();
        for (int attempt = 1; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            if (reservationRepository.findByReservationCode(reservationId).isPresent()) {
                log.warn("Reservation code collision, regenerating (attempt {}/{})", attempt + 1, MAX_CODE_GENERATION_ATTEMPTS);
                reservationId = ReservationUtils.generateReservationCode();
            } else {
                break;
            }
        }
        ShowtimeResponse showtime = cinemaApi.findShowtimeById(o.getShowtimeId());
        Map<String, String> initialData = new java.util.HashMap<>(Map.of(
                Constants.REDIS_RESERVATION_ID, reservationId,
                Constants.REDIS_USER_ID, o.getUserId().toString(),
                Constants.REDIS_SHOWTIME_ID, o.getShowtimeId().toString(),
                Constants.REDIS_THEATER_ID, o.getTheaterId().toString(),
                Constants.REDIS_PRICE, showtime.getPrice().toPlainString(),
                Constants.REDIS_SEAT_IDS, "[]"));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            initialData.put(Constants.REDIS_IDEMPOTENCY_KEY, idempotencyKey);
        }

        reservationRedisService.saveReservationInit(reservationId, initialData, HOLD_TIMEOUT);
        reservationRedisService.saveIdempotencyIndex(idempotencyKey, reservationId, HOLD_TIMEOUT);

        log.info("Reservation initialize, Id: {}, user Id: {}, showtime Id: {}, theater Id: {} , ttl: {}",
                reservationId,
                o.getUserId(),
                o.getShowtimeId(),
                o.getTheaterId(),
                HOLD_TIMEOUT.toSeconds());

        return ReservationInitResponse.builder()
                .reservationCode(reservationId)
                .showtimeId(o.getShowtimeId())
                .ttl(HOLD_TIMEOUT.toSeconds())
                .expiredAt(Instant.now().plusSeconds(HOLD_TIMEOUT.toSeconds()))
                .build();
    }

    private ReservationInitResponse findExistingByIdempotencyKey(String idempotencyKey, ReservationInitRequest o) {
        //  DB backstop first: an already persisted reservation wins over a stale Redis index
        String persistedCode = reservationRepository.findByIdempotencyKey(idempotencyKey)
                .map(Reservation::getReservationCode)
                .orElse(null);

        String reservationId = persistedCode != null
                ? persistedCode
                : reservationRedisService.getReservationIdByIdempotencyKey(idempotencyKey);

        if (reservationId == null) {
            return null;
        }
        Long ttl = reservationRedisService.getReservationTtl(reservationId);
        if (ttl == null || ttl <= 0) {
            log.warn("Reservation {} for idempotency key {} already expired, generating new one", reservationId, idempotencyKey);
            return null;
        }
        return ReservationInitResponse.builder()
                .reservationCode(reservationId)
                .showtimeId(o.getShowtimeId())
                .ttl(ttl)
                .expiredAt(Instant.now().plusSeconds(ttl))
                .build();
    }

    @Override
    @AuditLog(action = "'RESERVATION_HOLD'", entity = "'RESERVATION'", entityId = "#reservation.reservationId",
            toState = "'PENDING'",
            metadata = "'showtimeId=' + #reservation.showtimeId + ', seats=' + #reservation.seatIds")
    public void holdSeats(ReservationHoldSeatRequest reservation) {
        Long userId = reservation.getUserId();
        String reservationId = reservation.getReservationId();
        Long showtimeId = reservation.getShowtimeId();
        List<Long> seatIds = reservation.getSeatIds();
        log.info("Holding {} seats for reservation {} (user:{}, showtime: {})", seatIds.size(), reservationId, userId, showtimeId);

        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);

        Map<Object, Object> reservationData = reservationRedisService.getReservationData(reservationId);
        validateReservationOwnership(reservationData, reservationId, userId);
        validateShowtimeAndAvailability(showtimeId, seatIds.size());


        List<Long> allShowtimeSeatIds = cinemaApi.findSeatsByShowtime(showtimeId)
                .stream()
                .map(SeatResponse::getId)
                .toList();
        List<Long> oldSeatIds = reservationRedisService.parseSeatIdsFromReservationData(reservationData);
        List<Long> seatRelease = oldSeatIds
                .stream()
                .filter(id -> !seatIds.contains(id))
                .toList();
        if (!seatRelease.isEmpty()) {
            reservationRedisService.deleteSeatLocks(seatRelease, reservationId);
        }

        List<SeatResponse> seats = cinemaApi.findSeatsByIds(seatIds);
        validateSeatsForReservation(seats, showtimeId, seatIds);

        reservationRedisService.acquireSeatLock(seatIds, seats, redisKey);

        try {
            reservationRedisService.updateReservationSeats(reservationId, seatIds);

            //  Guard: never overwrite a row that already left PENDING (e.g. confirmed during this hold)
            reservationRepository.findByReservationCode(reservationId)
                    .filter(r -> r.getReservationStatus() != ReservationStatus.PENDING)
                    .ifPresent(r -> {
                        throw new ApiException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_INVALID_STATUS.format());
                    });

//        Upsert PENDING reservation row so ExpirationJob can track the hold
            reservationRepository.findByReservationCode(reservationId)
                    .ifPresentOrElse(
                            r -> {
                                //  Refresh the hold window: ExpirationJob clears PENDING rows past expiredAt
                                r.setExpiredAt(Instant.now().plus(HOLD_TIMEOUT));
                                reservationRepository.save(r);
                            },
                            () -> reservationRepository.save(Reservation.builder()
                                    .reservationCode(reservationId)
                                    .userId(userId)
                                    .showtimeId(showtimeId)
                                    .idempotencyKey((String) reservationData.get(Constants.REDIS_IDEMPOTENCY_KEY))
                                    .reservationStatus(ReservationStatus.PENDING)
                                    .totalAmount(java.math.BigDecimal.ZERO)
                                    .expiredAt(Instant.now().plus(HOLD_TIMEOUT))
                                    .isDeleted(false)
                                    .build()));

            reservationNotificationHelper.sendSeatHold(showtimeId, userId, allShowtimeSeatIds);
            log.info("Successfully hold {} seats with user id {} for reservation {}: {} ", seatIds.size(), userId, reservationId, seatIds);

        } catch (Exception e) {
            log.error("Error after locking seats, processing unlocking for reservation: {}", reservationId);
            reservationRedisService.deleteSeatLocks(seatIds, reservationId);
            throw e;
        }
    }

    private void validateSeatsForReservation(List<SeatResponse> seats, Long showtimeId, List<Long> seatIds) {
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats
                    .stream()
                    .map(SeatResponse::getId)
                    .toList();
            List<Long> notFoundSeatIds = seatIds
                    .stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .toList();
            throw new SeatUnavailableException(ErrorCode.SEAT_NOT_FOUND.format() + notFoundSeatIds);
        }
        List<String> wrongShowtimeSeats = new ArrayList<>();
        for (SeatResponse seat : seats) {
            if (!seat.getShowtimeId().equals(showtimeId)) {
                wrongShowtimeSeats.add(seat.getSeatNumber());
            }
        }

        if (!wrongShowtimeSeats.isEmpty()) {
            String wrongSeatNumbers = String.join(", ", wrongShowtimeSeats);
            throw new SeatUnavailableException(ErrorCode.SEAT_WRONG_SHOWTIME.format( wrongSeatNumbers));
        }

        List<String> bookedSeats = new ArrayList<>();
        for (SeatResponse seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                bookedSeats.add(seat.getSeatNumber());
            }
        }
        if (!bookedSeats.isEmpty()) {
            String bookedSeatNumbers = String.join(", ", bookedSeats);
            throw new SeatUnavailableException(ErrorCode.SEAT_UNAVAILABLE.format() + " " + bookedSeatNumbers);
        }

    }

    private void validateReservationOwnership(Map<Object, Object> reservationData, String reservationId, Long userId) {
        if (reservationData == null || reservationData.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND.format());
        }
        String userIdStr = (String) reservationData.get(Constants.REDIS_USER_ID);
        if (!userIdStr.equals(String.valueOf(userId))) {
            throw new PermissionDeniedException(ErrorCode.PERMISSION_FORBIDDEN.format());
        }
        String reservationIdStr = (String) reservationData.get(Constants.REDIS_RESERVATION_ID);
        if (reservationIdStr == null || !reservationIdStr.equals(reservationId)) {
            throw new ResourceNotFoundException(ErrorCode.RESERVATION_INVALID_DATA.format());
        }
        identityApi.findUserById(userId);
    }

    private void validateShowtimeAndAvailability(Long showtimeId, int requestSeats) {
        ShowtimeResponse showtime = cinemaApi.findShowtimeById(showtimeId);
        if (showtime.getShowDate().isBefore((LocalDate.now())) ||
                (showtime.getShowDate().isEqual(LocalDate.now()) &&
                        showtime.getShowTime().isBefore(LocalTime.now()))) {
            throw new IllegalStateException(ErrorCode.RESERVATION_PAST_SHOWTIME.format());
        }

        if (showtime.getAvailableSeats() < requestSeats) {
            throw new IllegalStateException(ErrorCode.RESERVATION_NOT_ENOUGH_SEATS.format( requestSeats, showtime.getAvailableSeats()));
        }
    }
}

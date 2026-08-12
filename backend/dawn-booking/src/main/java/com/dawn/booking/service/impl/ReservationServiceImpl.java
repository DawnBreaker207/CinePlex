package com.dawn.booking.service.impl;

import com.dawn.booking.client.MovieClientBookingService;
import com.dawn.booking.client.SeatClientService;
import com.dawn.booking.client.ShowtimeClientService;
import com.dawn.booking.client.UserClientService;
import com.dawn.booking.client.impl.VoucherClientServiceImpl;
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
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.helper.RedisKeyHelper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(15);

    ReservationRepository reservationRepository;

    SeatClientService seatService;

    UserClientService userService;

    ShowtimeClientService showtimeService;

    MovieClientBookingService movieService;

    ReservationNotificationHelper reservationNotificationHelper;

    ReservationRedisService reservationRedisService;

    VoucherClientServiceImpl voucherClientService;

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

        Map<Long, MovieDTO> movieMap = movieService.findAllByIds(movieIds)
                .stream()
                .collect(Collectors.toMap(MovieDTO::getId, s -> s));

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
                    MovieDTO movie = movieMap.get(showtime.getMovieId());
                    List<SeatDTO> seats = seatMap.getOrDefault(reservation.getId(), List.of());
                    return ReservationMappingHelper.toUserResponse(reservation, movie, showtime, seats);
                }));
    }

    @Override
    public ResponsePage<ReservationResponse> findAll(ReservationFilterRequest req, Pageable pageable) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

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
        MovieDTO movie = movieService.findOne(showtime.getMovieId());
        UserDTO user = userService.findById(reservation.getUserId());
        return ReservationMappingHelper.map(reservation, user, showtime, seats);
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

        String showtimeIdStr = (String) reservation.get("showtimeId");
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

        String reservationId = ReservationUtils.generateReservationIds();
        ShowtimeDTO showtime = showtimeService.findById(o.getShowtimeId());
        //        Create essential value to save on redis
        Map<String, String> initialData = Map.of(
                "reservationId", reservationId,
                "userId", o.getUserId().toString(),
                "showtimeId", o.getShowtimeId().toString(),
                "theaterId", o.getTheaterId().toString(),
                "price", showtime.getPrice().toPlainString(),
                "seatIds", "[]");

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

            reservationNotificationHelper.sendSeatHold(showtimeId, userId, allShowtimeSeatIds);
            log.info("Successfully hold {} seats with user id {} for reservation {}: {} ", seatIds.size(), userId, reservationId, seatIds);

        } catch (Exception e) {
            log.error("Error after locking seats, processing unlocking for reservation: {}", reservationId);
            reservationRedisService.deleteSeatLocks(seatIds, reservationId);
            throw e;
        }
    }

    @Override
    public ReservationResponse confirmReservation(String reservationId) {

        // Idempotency check
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent() && existing.get().getIsPaid()) {
            log.info("Reservation {} already confirmed, returning existing", reservationId);
            Reservation r = existing.get();
            List<SeatDTO> seats = seatService.findAllByReservationId(reservationId);
            ShowtimeDTO showtime = showtimeService.findById(r.getShowtimeId());
            UserDTO user = userService.findById(r.getUserId());
            return ReservationMappingHelper.map(r, user, showtime, seats);
        }

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


        validateSeatsStillAvailable(seatEntities);
        //  Calculate voucher, not used
        log.info("All {} seats verified as available in DB for reservation {}", seatEntities.size(), reservationId);
        String voucherCode = cachedData.getVoucherCode();
        BigDecimal originalAmount = showtime.getPrice().multiply(BigDecimal.valueOf(seatEntities.size()));
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal total = originalAmount;
        if (voucherCode != null && !voucherCode.isBlank()) {
            VoucherDiscountDTO finalCalc = voucherClientService.calculateVoucher(voucherCode, originalAmount);
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

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);

        // Update side effect
        try {
            updateSeatsAndShowtime(seatEntities, savedReservation);
        } catch (Exception e) {
            log.warn("Failed to update seats for reservation {}, will retry later", reservationId);

        }

        try {
            if (voucherCode != null && !voucherCode.isBlank()) {
                voucherClientService.useVoucher(voucherCode, cachedData.getUserId(), reservationId);
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
        log.info("Successfully confirmed reservation: {} with {} seats", reservation.getId(), seatEntities.size());
        return ReservationMappingHelper.map(savedReservation, user, showtime, seatEntities);

    }

    @Override
    public VoucherDiscountDTO applyVoucher(String reservationId, String code) {
        ReservationRedisDTO redisData = reservationRedisService.getFromRedis(reservationId);

        ShowtimeDTO showtime = showtimeService.findById(redisData.getShowtimeId());
        BigDecimal seatTotal = showtime.getPrice().multiply(BigDecimal.valueOf(redisData.getSeatsIds().size()));

        VoucherDiscountDTO discount = voucherClientService.calculateVoucher(code, seatTotal);

        redisData.setVoucherCode(code);
        reservationRedisService.saveVoucher(redisData);

        return discount;
    }

    @Override
    public void cancelReservation(String reservationId) {
        log.info("Processing cancellation tracking for: {}", reservationId);

        // Check confirm
        Optional<Reservation> existing = reservationRepository.findById(reservationId);
        if (existing.isPresent() && existing.get().getIsPaid()) {
            log.warn("Reservation {} already confirmed, skipping cancel", reservationId);
            return;
        }

        //        Get reservation id from redis
        ReservationRedisDTO cachedData = reservationRedisService.getFromRedis(reservationId);
        if (cachedData == null) {
            log.warn("Reservation {} not found in Redis, nothing to cancel", reservationId);
            return;
        }
        log.info("Get reservation from redis: {}", cachedData);

        //  Delete Redis lock
        List<Long> seatIds = cachedData.getSeatsIds();
        if (seatIds != null && !seatIds.isEmpty()) {
            reservationRedisService.deleteSeatLocks(seatIds, reservationId);
        }
        reservationRedisService.deleteReservation(reservationId);

        //   Save record CANCELLED
        if (existing.isEmpty()) {
            int seatCount = (seatIds != null) ? seatIds.size() : 0;
            BigDecimal price = new BigDecimal(cachedData.getPrice());
            BigDecimal total = price.multiply(BigDecimal.valueOf(seatCount));
            log.info("Calculated total amount: {} for {} seats", total, seatCount);
            Reservation reservation = Reservation
                    .builder()
                    .id(reservationId)
                    .userId(cachedData.getUserId())
                    .showtimeId(cachedData.getShowtimeId())
                    .reservationStatus(ReservationStatus.CANCELED)
                    .originalAmount(total)
                    .totalAmount(total)
                    .voucherCode(cachedData.getVoucherCode())
                    .isPaid(false)
                    .isDeleted(false)
                    .build();
            reservationRepository.save(reservation);
            log.info("Saved CANCELED reservation: {}", reservationId);
        }


        try {
            List<Long> allShowtimeSeatIds = seatService
                    .findAllByShowtimeId(cachedData.getShowtimeId())
                    .stream()
                    .map(SeatDTO::getId)
                    .toList();
            reservationNotificationHelper.getSeatRelease(cachedData.getShowtimeId(), cachedData.getUserId(), allShowtimeSeatIds);
            log.info("Published seat release event for showtime {}: {}", cachedData.getShowtimeId(), seatIds);
        } catch (Exception e) {
            log.warn("Failed to broadcast seat release for reservation {}", reservationId);

        }

        String voucherCode = cachedData.getVoucherCode();
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                voucherClientService.releaseVoucher(voucherCode, cachedData.getUserId());
            } catch (Exception e) {
                log.warn("Failed to release voucher {} for reservation {}", voucherCode, reservationId);
            }
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

    private void validateSeatsStillAvailable(List<SeatDTO> seats) {
        List<String> unavailableSeats = new ArrayList<>();
        for (SeatDTO seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                unavailableSeats.add(seat.getSeatNumber());
            }
        }
        if (!unavailableSeats.isEmpty()) {
            throw new SeatUnavailableException(Message.Exception.SEAT_UNAVAILABLE + " " + String.join(", ", unavailableSeats));
        }

    }

    private void validateReservationOwnership(Map<Object, Object> reservationData, String reservationId, Long userId) {
        log.info("Validate owner ship");
        if (reservationData == null || reservationData.isEmpty()) {
            throw new ResourceNotFoundException(Message.Exception.RESERVATION_NOT_FOUND);
        }
        String userIdStr = (String) reservationData.get("userId");
        log.info("User id {}", userIdStr);
        if (!userIdStr.equals(String.valueOf(userId))) {
            throw new PermissionDeniedException(Message.Exception.PERMISSION_FORBIDDEN);
        }
        //        Check and compare value valid
        String reservationIdStr = (String) reservationData.get("reservationId");
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

    private void updateSeatsAndShowtime(List<SeatDTO> seats, Reservation reservation) {
        ShowtimeDTO showtime = showtimeService.findById(reservation.getShowtimeId());
        int newAvailable = showtime.getAvailableSeats() - seats.size();
        if (newAvailable < 0) {
            log.error("Showtime {} available seats would become negative: {}, Current: {}", showtime.getId(), newAvailable, showtime.getAvailableSeats());
            return;
        }

        seats.forEach(seat -> {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setReservationId(reservation.getId());
        });

        //      Update Unavailable seats count in showtime
        seatService.saveAllSeat(seats);
        log.info("Updated {} seats to BOOKED status", seats.size());
        showtime.setAvailableSeats(newAvailable);
        showtimeService.save(showtime);
    }


}

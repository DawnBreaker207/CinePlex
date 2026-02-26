package com.dawn.booking.service;

import com.dawn.booking.dto.SeatLockFailure;
import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.SeatDTO;
import com.dawn.booking.dto.response.SseDTO;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.exception.wrapper.RedisStorageException;
import com.dawn.common.core.exception.wrapper.ReservationExpiredException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.common.infra.redis.service.RedisPublisher;
import com.dawn.common.infra.redis.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationRedisService {

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(15);

    RedisService redisService;

    RedisPublisher redisPublisher;

    ObjectMapper mapper;

    //    Reservation data
    public void saveReservationInit(String reservationId, Map<String, String> data, Duration ttl) {
        String key = RedisKeyHelper.reservationHoldKey(reservationId);
        redisService.putHash(key, data, ttl);
    }

    public Long getReservationTtl(String reservationId) {
        return redisService.getExpired(RedisKeyHelper.reservationHoldKey(reservationId));
    }

    public Map<Object, Object> getReservationData(String reservationId) {
        return redisService.getHash(RedisKeyHelper.reservationHoldKey(reservationId));
    }

    public void updateReservationSeats(String reservationId, List<Long> seats) {
        try {
            String key = RedisKeyHelper.reservationHoldKey(reservationId);
            Map<String, String> updates = new HashMap<>();

            updates.put("seatIds", mapper.writeValueAsString(seats));
            updates.put("voucherCode", "");

            redisService.putHash(key, updates, HOLD_TIMEOUT);
            log.info("Updated seats to redis success");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize seats for reservation {}: {}", reservationId, e.getMessage(), e);
        }
    }

    public void deleteReservation(String reservationId) {
        String key = RedisKeyHelper.reservationHoldKey(reservationId);
        redisService.delete(key);
    }

    //    Seat locking
    public Boolean lockSeat(Long seatId, String ownerKey, Duration ttl) {
        return redisService.setIfAbsent(RedisKeyHelper.seatLockKey(seatId), ownerKey, ttl);
    }

    public String getSeatOwner(Long seatId) {
        Object val = redisService.get(RedisKeyHelper.seatLockKey(seatId));
        return val != null ? String.valueOf(val) : null;
    }

    public void refreshSeatLockIfOwner(Long seatId, String ownerKey, Duration ttl) {
        String key = RedisKeyHelper.seatLockKey(seatId);
        String current = getSeatOwner(seatId);
        if (ownerKey.equals(current)) {
            redisService.expire(key, ttl);
        }
    }

    public Boolean deleteSeatLockIfOwner(Long seatId, String expectedOwner) {
        String key = RedisKeyHelper.seatLockKey(seatId);
        String current = (String) redisService.get(key);
        if (expectedOwner.equals(current)) {
            return redisService.delete(key);
        }
        return false;
    }

    //    Event publish
    public void publishSeatEvent(Long showtimeId, Map<String, Object> event) {
        try {
            String channel = RedisKeyHelper.showtimeChannel(showtimeId);
            log.info("Publish to Redis channel [{}]: {}", channel, event);
            redisPublisher.publish(channel, event);
            log.info("Successfully publish event {} to channel {}", event.get("event"), channel);
        } catch (Exception ex) {
            log.error("Failed to serialize event", ex);
            throw new RedisStorageException("Failed to store seat information. Please try again");
        }
    }

    public List<Long> acquireSeatLock(List<Long> seatIds, List<SeatDTO> seats, String redisKey) {
        List<Long> successfulSeatIds = new ArrayList<>();
        List<SeatLockFailure> failures = new ArrayList<>();
        //  Check seat in showtime was booked in redis
        for (Long seatId : seatIds) {
            //  Check seat lock in redis
            Boolean acquired = lockSeat(seatId, redisKey, HOLD_TIMEOUT);
            //  Get owner of this seat
            String owner = getSeatOwner(seatId);
            if (acquired) {
                successfulSeatIds.add(seatId);
                log.debug("Successfully locked seat {} for reservation {}", seatId, redisKey);
                continue;
                //  Check the right owner
            }
            if (redisKey.equals(owner)) {
                refreshSeatLockIfOwner(seatId, redisKey, HOLD_TIMEOUT);
                log.debug("Refreshed existing lock for seat {} in reservation {}", seatId, redisKey);
                successfulSeatIds.add(seatId);
            } else {
                //  Check if the seat was hold by another owner
                SeatDTO seat = seats
                        .stream()
                        .filter(s -> s
                                .getId()
                                .equals(seatId))
                        .findFirst()
                        .orElse(null);
                String seatNumber = seat != null ? seat.getSeatNumber() : seatId.toString();
                failures.add(new SeatLockFailure(seatId, seatNumber, owner));
                log.warn("Seat {} ({}) is hold by reservation {}", seatId, seatNumber, owner);
            }
        }

        //        If one of the list fail, return this exception
        if (!failures.isEmpty()) {
            log.warn("Failed to lock {} for seats for reservation {}. Rolling back {} successful locks", failures.size(), redisKey, successfulSeatIds.size());
            rollbackLocks(successfulSeatIds, redisKey);
            String failedSeatMessage = failures
                    .stream()
                    .map(f -> f.seatNumber() + " (held by " + f.ownerReservationId() + ")")
                    .collect(Collectors.joining((", ")));
            throw new SeatUnavailableException("Cannot hold seats. The following seats are currently hold by other users: " + failedSeatMessage);
        }
        return successfulSeatIds;
    }

    public void rollbackLocks(Collection<Long> seatIds, String expectedOwner) {
        if (seatIds == null || seatIds.isEmpty()) return;
        log.info("Rolling back {} seat lock for reservation {}", seatIds.size(), expectedOwner);
        for (Long seatId : seatIds) {
            try {
                String currentOwner = getSeatOwner(seatId);
                if (expectedOwner.equals(currentOwner)) {
                    deleteSeatLockIfOwner(seatId, expectedOwner);
                } else {
                    log.warn("Lock {} ownership changed during rollback. Expected: {}, Current: {}", seatId, expectedOwner, currentOwner);
                }
            } catch (Exception ex) {
                log.error("Failed to rollback lock {}", seatId, ex);
            }
        }
    }

    public void validateSeatLocks(String reservationId, List<Long> seatIds) {

        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        List<Long> expiredLocks = new ArrayList<>();
        List<Long> stolenLocks = new ArrayList<>();

        for (Long seatId : seatIds) {
            String lockOwner = getSeatOwner(seatId);
            if (lockOwner == null) {
                expiredLocks.add(seatId);
                log.warn("Lock expired for seat {} in reservation {}", seatId, reservationId);
            } else if (!lockOwner.equals(redisKey)) {
                stolenLocks.add(seatId);
                log.warn("Lock stolen for seat {} in reservation {}, Current owner: {}", seatId, reservationId, lockOwner);
            }
        }

        if (!expiredLocks.isEmpty() || !stolenLocks.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Cannot confirm reservation. ");

            if (!expiredLocks.isEmpty()) {
                errorMsg.append("Your hold on seats ").append(expiredLocks).append(" has expired. ");
            }
            if (!stolenLocks.isEmpty()) {
                errorMsg.append("Seats ").append(stolenLocks).append(" have been taken by other users. ");
            }

            errorMsg.append("Please select seat again");

            log.error("Reservation {} confirmation failed. Expired: {}, Stolen: {}", reservationId, expiredLocks, stolenLocks);
            throw new SeatUnavailableException(errorMsg.toString());
        }

        log.info("ALl Redis locks verified for reservation {}", reservationId);
    }

    public Long safeParseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Invalid {} format in Redis: {}", fieldName, value);
            throw new RedisStorageException("Invalid " + fieldName + " format in Redis");
        }
    }

    //        Clean up Redis
    public void cleanupRedisLocks(String reservationId, List<SeatDTO> seats) {
        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        int deletedLocks = 0;
        for (SeatDTO seat : seats) {
            Boolean deleted = deleteSeatLockIfOwner(seat.getId(), redisKey);
            if (deleted) {
                deletedLocks++;
                log.debug("Deleted lock for seat {}", seat.getId());
            } else {
                String currentOwner = getSeatOwner(seat.getId());
                log.warn("Lock for seat {} has unexpected owner: {}. Expected: {}", seat.getId(), currentOwner, redisKey);
            }
        }
        deleteReservation(reservationId);
        log.info("Cleaned up Redis: {} seat locks deleted, reservation key deleted", deletedLocks);
    }

    public void deleteSeatLocks(List<Long> seatIds, String reservationId) {
        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        for (Long seatId : seatIds) {
            deleteSeatLockIfOwner(seatId, redisKey);
        }
        log.info("ALl Redis locks verified for reservation {}", redisKey);
    }

    //    Get data from redis
    public ReservationRedisDTO getFromRedis(String reservationId) {
        Map<Object, Object> data = getReservationData(reservationId);
        log.info("Get from redis: {}", data);
        if (data == null || data.isEmpty()) {
            throw new ReservationExpiredException(Message.Exception.RESERVATION_EXPIRED);
        }


        Long userId = safeParseLong((String) data.get("userId"), "userId");
        Long showtimeId = safeParseLong((String) data.get("showtimeId"), "showtimeId");
        Long theaterId = safeParseLong((String) data.get("theaterId"), "theaterId");

        String voucherCode = (String) data.get("voucherCode");
        BigDecimal tempFinal = null;
        String tempFinalStr = (String) data.get("tempFinalAmount");
        if (tempFinalStr != null) {
            tempFinal = new BigDecimal(tempFinalStr);
        }

        List<Long> seatIds = Collections.emptyList();
        try {
            String seatJson = (String) data.get("seatIds");
            if (seatJson != null) {
                seatIds = mapper.readValue(seatJson, new TypeReference<>() {
                });
            }
        } catch (JsonProcessingException ex) {
            log.error("Error parsing seat IDs from Redis", ex);
            throw new RedisStorageException("Info in redis not exists or error when getting that");
        }
        return ReservationRedisDTO
                .builder()
                .id(reservationId)
                .userId(userId)
                .showtimeId(showtimeId)
                .theaterId(theaterId)
                .seatsIds(seatIds)
                .voucherCode(voucherCode)
                .build();
    }

    public List<Long> parseSeatIdsFromReservationData(Map<Object, Object> reservationData) {
        try {
            String currentSeatsJson = (String) reservationData.get("seatIds");
            log.info("Get current seat json: {}", currentSeatsJson);
            if (currentSeatsJson == null || currentSeatsJson.isEmpty()) {
                return Collections.emptyList();
            }
            return mapper.readValue(currentSeatsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RedisStorageException("Info in redis not exists or error when getting that");
        }
    }

    public void saveVoucher(ReservationRedisDTO data) {
        String key = RedisKeyHelper.reservationHoldKey(data.getId());

        Map<String, String> updates = new HashMap<>();

        if (data.getVoucherCode() != null) {
            updates.put("voucherCode", data.getVoucherCode());
        }

        try {
            updates.put("seatIds", mapper.writeValueAsString(data.getSeatsIds()));
        } catch (JsonProcessingException e) {
            log.error("Serialize seat ids failed", e);
        }

        redisService.putHash(key, updates, HOLD_TIMEOUT);
        log.info("Updated Redis for reservation {}: Voucher={}, TTL Reset", data.getId(), data.getVoucherCode());
    }

    public List<SseDTO> getLockedSeatsByShowtime(Long showtimeId) {
        if (showtimeId == null) return Collections.emptyList();
        List<SseDTO> seatState = new ArrayList<>();
        String pattern = "seat:locked:*";

        redisService.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(500)
                    .build();

            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);

                    //     Take data from Redis
                    Object rawValue = redisService.get(key);
                    if (rawValue == null) continue;

                    String reservationKey = rawValue.toString();
                    String reservationId = reservationKey.substring(reservationKey.lastIndexOf(":") + 1);


                    Map<Object, Object> reservationData = redisService.getHash(reservationKey);

                    if (reservationData.isEmpty()) continue;

                    String storedShowtimeId = (String) reservationData.get("showtimeId");

                    if (!showtimeId.toString().equals(storedShowtimeId)) {
                        continue;
                    }

                    Long seatId = Long.parseLong(key.substring(key.lastIndexOf(":") + 1));

                    SseDTO initialState = SseDTO
                            .builder()
                            .seatId(seatId)
                            .reservationId(reservationId)
                            .build();

                    seatState.add(initialState);
                }
            }
            return null;
        });

        return seatState;
    }

}

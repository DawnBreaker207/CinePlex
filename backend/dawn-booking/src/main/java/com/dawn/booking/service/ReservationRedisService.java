package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.booking.dto.response.SseDTO;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.ErrorCode;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ReservationRedisService {

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(Constants.RESERVATION_HOLD_MINUTES);

    RedisService redisService;

    RedisPublisher redisPublisher;

    ObjectMapper mapper;

    public void saveReservationInit(String reservationId, Map<String, String> data, Duration ttl) {
        String key = RedisKeyHelper.reservationHoldKey(reservationId);
        redisService.putHash(key, data, ttl);
    }

    public void saveIdempotencyIndex(String idempotencyKey, String reservationId, Duration ttl) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        redisService.set(RedisKeyHelper.reservationIdempotencyKey(idempotencyKey), reservationId, ttl);
    }

    public String getReservationIdByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        Object val = redisService.get(RedisKeyHelper.reservationIdempotencyKey(idempotencyKey));
        return val != null ? String.valueOf(val) : null;
    }

    public void deleteIdempotencyIndex(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        redisService.delete(RedisKeyHelper.reservationIdempotencyKey(idempotencyKey));
    }

    public Long getReservationTtl(String reservationId) {
        return redisService.getTimeToLive(RedisKeyHelper.reservationHoldKey(reservationId));
    }

    public Map<Object, Object> getReservationData(String reservationId) {
        return redisService.getHash(RedisKeyHelper.reservationHoldKey(reservationId));
    }

    public void updateReservationSeats(String reservationId, List<Long> seats) {
        try {
            String key = RedisKeyHelper.reservationHoldKey(reservationId);
            Map<Object, Object> existing = redisService.getHash(key);
            Map<String, String> updates = new HashMap<>();

            updates.put(Constants.REDIS_SEAT_IDS, mapper.writeValueAsString(seats));
            if (existing != null && existing.get(Constants.REDIS_VOUCHER_CODE) != null) {
                updates.put(Constants.REDIS_VOUCHER_CODE, (String) existing.get(Constants.REDIS_VOUCHER_CODE));
            }

            redisService.putHash(key, updates, HOLD_TIMEOUT);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize seats for reservation {}: {}", reservationId, e.getMessage(), e);
        }
    }

    public void deleteReservation(String reservationId) {
        String key = RedisKeyHelper.reservationHoldKey(reservationId);
        redisService.delete(key);
    }

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
        return redisService.releaseLock(key, expectedOwner);
    }

    public void publishSeatEvent(Long showtimeId, Map<String, Object> event) {
        try {
            String channel = RedisKeyHelper.showtimeChannel(showtimeId);
            redisPublisher.publish(channel, event);
            log.info("Successfully publish event {} to channel {}", event.get("event"), channel);
        } catch (Exception ex) {
            log.error("Failed to serialize event", ex);
            throw new RedisStorageException(ErrorCode.FAILED_STORE_SEAT.format());
        }
    }

    public List<Long> acquireSeatLock(List<Long> seatIds, List<SeatResponse> seats, String redisKey) {
        List<String> keys = seatIds
                .stream()
                .map(RedisKeyHelper::seatLockKey)
                .toList();

        List<Object> result = redisService.lockMulti(keys, redisKey, HOLD_TIMEOUT);

        if (result == null || result.isEmpty() || result.getFirst() == null) {
            log.error("Lua script returned null or empty result for reservation {}", redisKey);
            throw new SeatUnavailableException(ErrorCode.FAILED_SEAT_LOCK.format());
        }

        Long status = Long.parseLong(result.getFirst().toString());

        if (status == 1) {
            log.info("Successfully locked {} seats for reservation {}", seatIds.size(), redisKey);
            return seatIds;
        } else {
            String failedKey = result.size() > 1 && result.get(1) != null ? result.get(1).toString() : "unknown";
            String currentOwner = result.size() > 2 && result.get(2) != null ? result.get(2).toString() : "unknown";

            String failedSeatIdStr = failedKey.contains(":") ? failedKey.substring(failedKey.lastIndexOf(":") + 1) : failedKey;
            Long parsedSeatId = null;
            try {

                parsedSeatId = Long.parseLong(failedSeatIdStr);
            } catch (NumberFormatException e) {
                log.warn("Could not parse failedSeatId from key: {}", failedKey);

            }
            final Long failedSeatId = parsedSeatId;
            String seatNumber = failedSeatId != null
                    ? seats.stream()
                    .filter(s -> s.getId().equals(failedSeatId))
                    .findFirst()
                    .map(SeatResponse::getSeatNumber)
                    .orElse(failedSeatIdStr)
                    : failedSeatIdStr;

            log.warn("Bulk lock failed! Seat {} ({}) is held by {}", failedSeatId, seatNumber, currentOwner);
            throw new SeatUnavailableException(ErrorCode.SEAT_HELD_BY_ANOTHER.format( seatNumber));
        }
    }

    public void validateSeatLocks(String reservationId, List<Long> seatIds) {

        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        List<Long> expiredLocks = new ArrayList<>();
        List<Long> stolenLocks = new ArrayList<>();

        List<String> keys = seatIds.stream().map(RedisKeyHelper::seatLockKey).toList();
        List<Object> owners = redisService.multiGet(keys);

        for (int i = 0; i < seatIds.size(); i++) {
            Long seatId = seatIds.get(i);
            Object lockOwner = owners.get(i);

            if (lockOwner == null) {
                expiredLocks.add(seatId);
                log.warn("Lock expired for seat {} in reservation {}", seatId, reservationId);
            } else {
                String lockOwnerStr = lockOwner.toString();
                // Lock value is now a fencing token "<owner>:<epoch>", compare against the base key
                if (!lockOwnerStr.equals(redisKey) && !lockOwnerStr.startsWith(redisKey + ":")) {
                    stolenLocks.add(seatId);
                    log.warn("Lock stolen for seat {} in reservation {}, Current owner: {}", seatId, reservationId, lockOwner);
                }
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

    }

    public Long safeParseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Invalid {} format in Redis: {}", fieldName, value);
            throw new RedisStorageException(ErrorCode.INVALID_REDIS_FORMAT.format( fieldName));
        }
    }

    public void cleanupRedisLocks(String reservationId, List<SeatResponse> seats) {
        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        int deletedLocks = 0;
        for (SeatResponse seat : seats) {
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
    }

    public void deleteSeatLocks(List<Long> seatIds, String reservationId) {
        String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
        for (Long seatId : seatIds) {
            deleteSeatLockIfOwner(seatId, redisKey);
        }
    }

    public ReservationRedisDTO getReservationSession(String reservationId) {
        Map<Object, Object> data = getReservationData(reservationId);
        if (data == null || data.isEmpty()) {
            throw new ReservationExpiredException(ErrorCode.RESERVATION_EXPIRED.format());
        }


        Long userId = safeParseLong((String) data.get(Constants.REDIS_USER_ID), Constants.REDIS_USER_ID);
        Long showtimeId = safeParseLong((String) data.get(Constants.REDIS_SHOWTIME_ID), Constants.REDIS_SHOWTIME_ID);
        Long theaterId = safeParseLong((String) data.get(Constants.REDIS_THEATER_ID), Constants.REDIS_THEATER_ID);

        String voucherCode = (String) data.get(Constants.REDIS_VOUCHER_CODE);
        String idempotencyKey = (String) data.get(Constants.REDIS_IDEMPOTENCY_KEY);
        BigDecimal tempFinal = null;
        String tempFinalStr = (String) data.get(Constants.REDIS_TEMP_FINAL_AMOUNT);
        if (tempFinalStr != null) {
            tempFinal = new BigDecimal(tempFinalStr);
        }

        String price = (String) data.get(Constants.REDIS_PRICE);

        List<Long> seatIds = Collections.emptyList();
        try {
            String seatJson = (String) data.get(Constants.REDIS_SEAT_IDS);
            if (seatJson != null) {
                seatIds = mapper.readValue(seatJson, new TypeReference<>() {
                });
            }
        } catch (JsonProcessingException ex) {
            log.error("Error parsing seat IDs from Redis", ex);
            throw new RedisStorageException(ErrorCode.INVALID_REDIS_DATA.format());
        }
        return ReservationRedisDTO
                .builder()
                .id(reservationId)
                .userId(userId)
                .showtimeId(showtimeId)
                .theaterId(theaterId)
                .seatsIds(seatIds)
                .voucherCode(voucherCode)
                .idempotencyKey(idempotencyKey)
                .price(price)
                .build();
    }

    //    Processing lock (short TTL, guards the DB write in confirmReservation)
    public boolean tryAcquireProcessingLock(String reservationId) {
        return redisService.setIfAbsent("processing:" + reservationId, reservationId,
                Duration.ofSeconds(Constants.PROCESSING_LOCK_TTL_SECONDS));
    }

    public void releaseProcessingLock(String reservationId) {
        redisService.delete("processing:" + reservationId);
    }

    public List<Long> parseSeatIdsFromReservationData(Map<Object, Object> reservationData) {
        try {
            String currentSeatsJson = (String) reservationData.get(Constants.REDIS_SEAT_IDS);
            if (currentSeatsJson == null || currentSeatsJson.isEmpty()) {
                return Collections.emptyList();
            }
            return mapper.readValue(currentSeatsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RedisStorageException(ErrorCode.INVALID_REDIS_DATA.format());
        }
    }

    public void saveVoucher(ReservationRedisDTO data) {
        String key = RedisKeyHelper.reservationHoldKey(data.getId());

        Map<String, String> updates = new HashMap<>();

        if (data.getVoucherCode() != null) {
            updates.put(Constants.REDIS_VOUCHER_CODE, data.getVoucherCode());
        }

        try {
            updates.put(Constants.REDIS_SEAT_IDS, mapper.writeValueAsString(data.getSeatsIds()));
        } catch (JsonProcessingException e) {
            log.error("Serialize seat ids failed", e);
        }

        redisService.putHash(key, updates, HOLD_TIMEOUT);
        log.info("Updated Redis for reservation {}: Voucher={}, TTL Reset", data.getId(), data.getVoucherCode());
    }

    public List<SseDTO> getLockedSeatsByShowtime(Long showtimeId, List<Long> showtimeSeatIds) {
        if (showtimeId == null) return Collections.emptyList();
        List<String> keys = showtimeSeatIds.stream().map(RedisKeyHelper::seatLockKey).toList();

        List values = redisService.multiGet(keys);

        List<SseDTO> seatState = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            Object value = values.get(i);

            if (value != null) {
                Long seatId = showtimeSeatIds.get(i);

                String reservationKey = value.toString();
                // Strip optional fencing epoch suffix, then extract the reservation id after the last ":"
                int lastColon = reservationKey.lastIndexOf(":");
                if (lastColon >= 0 && reservationKey.substring(lastColon + 1).chars().allMatch(Character::isDigit)) {
                    reservationKey = reservationKey.substring(0, lastColon);
                }
                String reservationId = reservationKey.substring(reservationKey.lastIndexOf(":") + 1);

                seatState.add(SseDTO.builder().seatId(seatId).reservationId(reservationId).build());
            }
        }
        return seatState;
    }

}

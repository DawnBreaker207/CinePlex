package com.dawn.booking.service.impl;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.ReservationRedisService;
import com.dawn.booking.service.VoucherApplicationService;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.infra.redis.service.VelocityGuard;
import org.springframework.http.HttpStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class VoucherApplicationServiceImpl implements VoucherApplicationService {

    CinemaModuleApi cinemaApi;

    ReservationRedisService reservationRedisService;

    CatalogModuleApi catalogApi;

    ReservationRepository reservationRepository;

    VelocityGuard velocityGuard;

    static Duration HOLD_TIMEOUT = Duration.ofMinutes(com.dawn.common.core.constant.Constants.RESERVATION_HOLD_MINUTES);

    @Override
    public VoucherDiscountDTO applyVoucher(String reservationId, String code) {
        // Rate-limit brute force attempts (learned from production Auction/Go specs)
        if (!velocityGuard.tryAcquire("rate:voucher:" + reservationId, 5, Duration.ofSeconds(60))) {
            log.warn("Rate limit exceeded for voucher application on reservation {}", reservationId);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    com.dawn.common.core.constant.ErrorCode.VOUCHER_RATE_LIMITED.format());
        }

        // S4: refuse to discount a dead hold (laptop sleep / expired TTL).
        // Matches the createPayment guard in the payment module.
        Long holdTtl = reservationRedisService.getReservationTtl(reservationId);
        if (holdTtl == null || holdTtl <= 0) {
            log.warn("Voucher refused for reservation {}: hold already expired", reservationId);
            throw new ApiException(HttpStatus.CONFLICT,
                    com.dawn.common.core.constant.ErrorCode.RESERVATION_EXPIRED.format());
        }
        ReservationRedisDTO redisData = reservationRedisService.getReservationSession(reservationId);
        if (redisData == null) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, com.dawn.common.core.constant.ErrorCode.RESERVATION_NOT_FOUND.format());
        }

        ShowtimeResponse showtime = cinemaApi.findShowtimeById(redisData.getShowtimeId());
        BigDecimal seatTotal = cinemaApi.findSeatsByIds(redisData.getSeatsIds())
                .stream()
                .map(SeatResponse::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VoucherCalculation calc = catalogApi.calculateVoucher(code, seatTotal);
        VoucherDiscountDTO discount = VoucherDiscountDTO.builder()
                .code(calc.getCode())
                .originalAmount(calc.getOriginalAmount())
                .discountAmount(calc.getDiscountAmount())
                .finalAmount(calc.getFinalAmount())
                .build();

        redisData.setVoucherCode(code);
        reservationRedisService.saveVoucher(redisData);

        //  Reset the DB-side hold window together with the Redis TTL refresh
        reservationRepository.findByReservationCode(reservationId)
                .ifPresent(r -> {
                    r.setExpiredAt(Instant.now().plus(HOLD_TIMEOUT));
                    reservationRepository.save(r);
                });

        return discount;
    }

    public void releaseVoucher(ReservationRedisDTO cachedData, Optional<Reservation> existing) {
        String voucherCode = cachedData != null ? cachedData.getVoucherCode()
                : existing.map(Reservation::getVoucherCode).orElse(null);
        Long voucherUserId = cachedData != null ? cachedData.getUserId()
                : existing.map(Reservation::getUserId).orElse(null);
        if (voucherCode != null && !voucherCode.isBlank() && voucherUserId != null) {
            try {
                catalogApi.releaseVoucher(voucherCode, voucherUserId);
            } catch (Exception e) {
                log.warn("Failed to release voucher for reservation {}", existing.map(Reservation::getReservationCode).orElse("?"));
            }
        }
    }
}

package com.dawn.booking.service;

import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.common.core.constant.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationJob {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.expiration.scan-interval-ms:60000}")
    public void expirePendingReservations() {
        List<Reservation> expired = reservationRepository
                .findAllByReservationStatusAndExpiredAtBefore(ReservationStatus.PENDING, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("Found {} expired PENDING reservations to release", expired.size());
        for (Reservation reservation : expired) {
            try {
                reservationService.expireReservation(reservation.getId());
            } catch (Exception e) {
                log.error("Failed to expire reservation {}", reservation.getId(), e);
            }
        }
    }
}

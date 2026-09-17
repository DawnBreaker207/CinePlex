package com.dawn.booking.service;

import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.common.core.constant.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpirationJob {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.expiration.scan-interval-ms:60000}")
    @Transactional
    public void expirePendingReservations() {
        //  SKIP LOCKED: each expired row is locked for this job run; other nodes skip it
        List<Reservation> expired = reservationRepository
                .findExpiredLockedSkipped(ReservationStatus.PENDING, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        log.debug("Found {} expired PENDING reservations to release", expired.size());
        for (Reservation reservation : expired) {
            try {
                reservationService.expire(reservation.getReservationCode());
            } catch (ObjectOptimisticLockingFailureException e) {
                // Another node (or a concurrent confirm) already transitioned this reservation
                log.debug("Reservation {} expired concurrently, skipping", reservation.getReservationCode());
            } catch (Exception e) {
                log.error("Failed to expire reservation {}", reservation.getId(), e);
            }
        }
    }
}

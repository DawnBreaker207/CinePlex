package com.dawn.booking.job;

import com.dawn.booking.client.SeatClientService;
import com.dawn.booking.dto.response.SeatDTO;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationJob {

    private final ReservationRepository reservationRepository;
    private final SeatClientService seatService;
    private final CatalogModuleApi catalogApi;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelayString = "${app.reservation.expiration-interval-ms:60000}")
    public void expirePendingReservations() {
        List<Reservation> expired = reservationRepository
                .findAllByReservationStatusAndExpiredAtBefore(ReservationStatus.PENDING, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("Expiring {} pending reservations", expired.size());
        for (Reservation r : expired) {
            try {
                r.setReservationStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(r);
                auditLogService.record("RESERVATION_EXPIRED", "RESERVATION", r.getId(),
                        ReservationStatus.PENDING.name(), ReservationStatus.EXPIRED.name(), null);

                List<SeatDTO> seats = seatService.findAllByReservationId(r.getId());
                if (!seats.isEmpty()) {
                    List<Long> seatIds = seats.stream().map(SeatDTO::getId).toList();
                    seatService.unbookSeats(r.getId(), seatIds);
                    log.info("Released {} seats for expired reservation {}", seatIds.size(), r.getId());
                }

                if (r.getVoucherCode() != null && !r.getVoucherCode().isBlank()) {
                    catalogApi.releaseVoucher(r.getVoucherCode(), r.getUserId());
                }
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}
package com.dawn.booking.service;

import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpirationJob")
class ExpirationJobTest {

    @Mock
    ReservationRepository reservationRepository;

    @Mock
    ReservationLifecycleService reservationService;

    @InjectMocks
    ExpirationJob job;

    @Test
    @DisplayName("expired PENDING reservation  release")
    void expirePending_shouldRelease() {
        Reservation reservation = ReservationTestData.buildReservation("CP-1", false);
        when(reservationRepository.findAllByReservationStatusAndExpiredAtBefore(any(), any(Instant.class)))
                .thenReturn(List.of(reservation));

        job.expirePendingReservations();

        verify(reservationService).expireReservation("CP-1");
    }

    @Test
    @DisplayName("concurrent transition (optimistic lock)  skip, keep scanning")
    void expirePending_optimisticLockOnOne_shouldSkipAndContinue() {
        Reservation r1 = ReservationTestData.buildReservation("CP-1", false);
        Reservation r2 = ReservationTestData.buildReservation("CP-2", false);
        when(reservationRepository.findAllByReservationStatusAndExpiredAtBefore(any(), any(Instant.class)))
                .thenReturn(List.of(r1, r2));
        doThrow(new ObjectOptimisticLockingFailureException(Reservation.class.getName(), "CP-1"))
                .when(reservationService).expireReservation("CP-1");

        assertThatCode(() -> job.expirePendingReservations()).doesNotThrowAnyException();

        verify(reservationService).expireReservation("CP-2");
    }
}

package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.ReservationResponse;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.impl.ReservationLifecycleServiceImpl;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.common.core.aspect.AuditLogAspect;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.ApiException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationLifecycleServiceImpl")
class ReservationLifecycleServiceImplTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    CinemaModuleApi cinemaApi;
    @Mock
    IdentityModuleApi identityApi;
    @Mock
    CatalogModuleApi catalogApi;
    @Mock
    ReservationNotificationHelper notificationHelper;
    @Mock
    ReservationRedisService reservationRedisService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    VoucherApplicationService voucherApplicationService;

    @InjectMocks
    ReservationLifecycleServiceImpl service;

    @BeforeEach
    void setUpProxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAspect(new AuditLogAspect(auditLogService));
        service = (ReservationLifecycleServiceImpl) factory.getProxy();
    }

    @Nested
    @DisplayName("confirmReservation")
    class ConfirmReservation {

        @Test
        @DisplayName("idempotency: already confirmed → return response, no re-save")
        void confirmReservation_alreadyPaid_shouldReturnExisting() {
            Reservation existing = ReservationTestData.buildReservation("RES-001", true);
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.of(existing));
            when(cinemaApi.findSeatsByReservationId("RES-001")).thenReturn(List.of());
            when(cinemaApi.findShowtimeById(10L)).thenReturn(ReservationTestData.buildShowtime(10L));
            when(identityApi.findUserById(1L)).thenReturn(ReservationTestData.buildUser(1L));

            ReservationResponse result = service.confirmReservation("RES-001");

            assertThat(result).isNotNull();
            verify(reservationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("successful flow → save Reservation CONFIRMED, isPaid=true")
        void confirmReservation_success_shouldSaveConfirmedReservation() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());
            stubRedisData("RES-001", List.of(101L, 102L));
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            UserResponse user = ReservationTestData.buildUser(1L);
            when(identityApi.findUserById(1L)).thenReturn(user);

            List<SeatResponse> seats = List.of(
                    ReservationTestData.buildSeat(101L, 10L), ReservationTestData.buildSeat(102L, 10L));
            when(cinemaApi.findSeatsByIdWithLock(anyList())).thenReturn(seats);

            ShowtimeResponse showtime = ReservationTestData.buildShowtime(10L);
            when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);
            when(cinemaApi.bookSeats(10L, List.of(101L, 102L), "RES-001")).thenReturn(2);

            Reservation saved = ReservationTestData.buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);

            doNothing().when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());

            service.confirmReservation("RES-001");

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).saveAndFlush(captor.capture());
            Reservation toSave = captor.getValue();
            assertThat(toSave.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(toSave.getIsPaid()).isTrue();
        }

        @Test
        @DisplayName("side effect email fails → reservation still confirmed (no rollback)")
        void confirmReservation_emailFails_shouldNotRollback() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());
            stubRedisData("RES-001", List.of(101L));
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            when(identityApi.findUserById(1L)).thenReturn(ReservationTestData.buildUser(1L));
            when(cinemaApi.findSeatsByIdWithLock(anyList())).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));
            ShowtimeResponse showtime = ReservationTestData.buildShowtime(10L);
            when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);
            when(cinemaApi.bookSeats(10L, List.of(101L), "RES-001")).thenReturn(1);

            Reservation saved = ReservationTestData.buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);

            doThrow(new RuntimeException("SMTP down"))
                    .when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());

            assertThatNoException().isThrownBy(() -> service.confirmReservation("RES-001"));

            verify(reservationRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("seat no longer AVAILABLE on confirm → throw SeatUnavailableException")
        void confirmReservation_seatBecomesUnavailable_shouldThrow() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());
            stubRedisData("RES-001", List.of(101L));
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            when(identityApi.findUserById(1L)).thenReturn(ReservationTestData.buildUser(1L));

            SeatResponse bookedSeat = ReservationTestData.buildSeat(101L, 10L);
            bookedSeat.setStatus(SeatStatus.BOOKED);
            when(cinemaApi.findSeatsByIdWithLock(anyList())).thenReturn(List.of(bookedSeat));
            when(cinemaApi.findShowtimeById(10L)).thenReturn(ReservationTestData.buildShowtime(10L));
            when(cinemaApi.bookSeats(10L, List.of(101L), "RES-001")).thenReturn(0);

            assertThatThrownBy(() -> service.confirmReservation("RES-001"))
                    .isInstanceOf(SeatUnavailableException.class);

            verify(reservationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("has voucher → calculateVoucher called, discountAmount correct")
        void confirmReservation_withVoucher_shouldApplyDiscount() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            when(identityApi.findUserById(1L)).thenReturn(ReservationTestData.buildUser(1L));
            when(cinemaApi.findSeatsByIdWithLock(anyList())).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));
            ShowtimeResponse showtime = ReservationTestData.buildShowtime(10L); // price = 100_000
            when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);
            when(cinemaApi.bookSeats(10L, List.of(101L), "RES-001")).thenReturn(1);

            VoucherCalculation discount = VoucherCalculation.builder()
                    .discountAmount(new BigDecimal("10000"))
                    .finalAmount(new BigDecimal("90000"))
                    .build();
            when(catalogApi.calculateVoucher(eq("DAWN10"), any())).thenReturn(discount);
            doNothing().when(catalogApi).useVoucher(eq("DAWN10"), eq(1L), anyString());

            Reservation saved = ReservationTestData.buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);
            doNothing().when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());

            service.confirmReservation("RES-001");

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getDiscountAmount()).isEqualByComparingTo("10000");
            assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("90000");
        }

        @Test
        @DisplayName("useVoucher fails → reservation still confirmed (side effect isolation)")
        void confirmReservation_voucherUseFails_shouldNotRollback() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            when(identityApi.findUserById(1L)).thenReturn(ReservationTestData.buildUser(1L));
            when(cinemaApi.findSeatsByIdWithLock(anyList())).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));
            ShowtimeResponse showtime = ReservationTestData.buildShowtime(10L);
            when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);
            when(cinemaApi.bookSeats(10L, List.of(101L), "RES-001")).thenReturn(1);
            when(catalogApi.calculateVoucher(any(), any())).thenReturn(
                    VoucherCalculation.builder()
                            .discountAmount(BigDecimal.ZERO)
                            .finalAmount(new BigDecimal("100000"))
                            .build());

            doThrow(new RuntimeException("Voucher service down"))
                    .when(catalogApi).useVoucher(anyString(), anyLong(), anyString());

            Reservation saved = ReservationTestData.buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);
            doNothing().when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());

            assertThatNoException().isThrownBy(() -> service.confirmReservation("RES-001"));
            verify(reservationRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("empty seatIds in Redis → throw IllegalStateException")
        void confirmReservation_noSeats_shouldThrow() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(Collections.emptyList()).build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(reservationRedisService.tryAcquireProcessingLock("RES-001")).thenReturn(true);

            assertThatThrownBy(() -> service.confirmReservation("RES-001"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancelReservation")
    class CancelReservation {

        @Test
        @DisplayName("already CONFIRMED → throw conflict, don't touch Redis/DB")
        void cancelReservation_alreadyPaid_shouldThrow() {
            when(reservationRepository.findById("RES-001"))
                    .thenReturn(Optional.of(ReservationTestData.buildReservation("RES-001", true)));

            assertThatThrownBy(() -> service.cancelReservation("RES-001"))
                    .isInstanceOf(ApiException.class);

            verify(reservationRedisService, never()).deleteReservation(any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("cancel success → delete Redis lock, save CANCELED")
        void cancelReservation_success_shouldCleanRedisAndSaveRecord() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L, 102L)).price("100000").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(
                    List.of(ReservationTestData.buildSeat(101L, 10L), ReservationTestData.buildSeat(102L, 10L)));

            service.cancelReservation("RES-001");

            verify(reservationRedisService).deleteSeatLocks(eq(List.of(101L, 102L)), eq("RES-001"));
            verify(reservationRedisService).deleteReservation("RES-001");

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getReservationStatus()).isEqualTo(ReservationStatus.CANCELED);
            assertThat(captor.getValue().getIsPaid()).isFalse();
        }

        @Test
        @DisplayName("has voucher → releaseVoucher called on cancel")
        void cancelReservation_withVoucher_shouldReleaseVoucher() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").price("100000").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));

            service.cancelReservation("RES-001");

            verify(voucherApplicationService).releaseVoucher(eq(redisData), any());
        }

        @Test
        @DisplayName("voucher release fails → does not affect cancel flow")
        void cancelReservation_releaseVoucherFails_shouldNotThrow() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").price("100000").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));
            doThrow(new RuntimeException("Voucher service down"))
                    .when(voucherApplicationService).releaseVoucher(any(), any());

            assertThatNoException().isThrownBy(() -> service.cancelReservation("RES-001"));
        }

        @Test
        @DisplayName("force cancel → unbook seats, release voucher, audit with old state, broadcast")
        void forceCancelReservation_shouldCleanupSeatsVoucherAndAudit() {
            when(reservationRepository.findById("RES-001"))
                    .thenReturn(Optional.of(ReservationTestData.buildReservation("RES-001", true)));
            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").price("100000").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(cinemaApi.findSeatsByShowtime(10L))
                    .thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));

            service.forceCancelReservation("RES-001");

            verify(cinemaApi).unbookSeats("RES-001", List.of(101L));
            verify(voucherApplicationService).releaseVoucher(eq(redisData), any());
            verify(auditLogService).record(eq("RESERVATION_CANCELED"), eq("RESERVATION"), eq("RES-001"),
                    eq("CONFIRMED"), eq("CANCELED"), anyString());
            verify(notificationHelper).getSeatRelease(eq(10L), eq(1L), any());
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void stubRedisData(String reservationId, List<Long> seatIds) {
        when(reservationRedisService.getFromRedis(reservationId))
                .thenReturn(ReservationTestData.buildRedisData(reservationId, seatIds));
    }
}
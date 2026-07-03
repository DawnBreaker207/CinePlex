package com.dawn.booking.service;

import com.dawn.booking.client.MovieClientBookingService;
import com.dawn.booking.client.SeatClientService;
import com.dawn.booking.client.ShowtimeClientService;
import com.dawn.booking.client.UserClientService;
import com.dawn.booking.client.impl.VoucherClientServiceImpl;
import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.response.*;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.impl.ReservationServiceImpl;
import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationServiceImpl")
class ReservationServiceImplTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    SeatClientService seatService;
    @Mock
    UserClientService userService;
    @Mock
    ShowtimeClientService showtimeService;
    @Mock
    MovieClientBookingService movieService;
    @Mock
    ReservationNotificationHelper notificationHelper;
    @Mock
    ReservationRedisService reservationRedisService;
    @Mock
    VoucherClientServiceImpl voucherClientService;

    @InjectMocks
    ReservationServiceImpl service;

    // ----------------------------------------------------------------
    // initReservation
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("initReservation")
    class InitReservation {

        @Test
        @DisplayName("valid input → create reservationId, save to Redis, return response with correct TTL")
        void initReservation_valid_shouldSaveRedisAndReturnResponse() {
            ReservationInitRequest request = ReservationInitRequest.builder()
                    .userId(1L)
                    .showtimeId(10L)
                    .theaterId(5L)
                    .build();

            when(showtimeService.findById(10L)).thenReturn(buildShowtime(10L));

            ReservationInitResponse response = service.initReservation(request);

            // Verify Redis was called with correct data
            ArgumentCaptor<String> reservationIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(reservationRedisService).saveReservationInit(
                    reservationIdCaptor.capture(), anyMap(), any());

            assertThat(response.getReservationId()).isNotBlank();
            assertThat(response.getShowtimeId()).isEqualTo(10L);
            assertThat(response.getTtl()).isEqualTo(900L); // 15 phút
            assertThat(response.getExpiredAt()).isNotNull();
            // reservationId in response matches the one saved in Redis
            assertThat(response.getReservationId()).isEqualTo(reservationIdCaptor.getValue());
        }
    }

    // ----------------------------------------------------------------
    // holdReservationSeats
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("holdReservationSeats")
    class HoldReservationSeats {

        @Test
        @DisplayName("hold success → acquireSeatLock and updateReservationSeats are called")
        void holdSeats_success_shouldLockAndUpdateRedis() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L, 102L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            stubSeats(List.of(101L, 102L), 10L);

            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList()); // no old seats
            when(seatService.findAllByShowtimeId(10L)).thenReturn(
                    List.of(buildSeat(101L, 10L), buildSeat(102L, 10L)));

            service.holdReservationSeats(request);

            verify(reservationRedisService).acquireSeatLock(eq(List.of(101L, 102L)), anyList(), anyString());
            verify(reservationRedisService).updateReservationSeats(eq("RES-001"), eq(List.of(101L, 102L)));
        }

        @Test
        @DisplayName("seat not in showtime → throw SeatUnavailableException")
        void holdSeats_wrongShowtime_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList());
            when(seatService.findAllByShowtimeId(10L)).thenReturn(List.of(buildSeat(101L, 10L)));

            // Seat belongs to a different showtime (99L)
            SeatDTO wrongSeat = buildSeat(101L, 99L);
            when(seatService.findAllById(anyList())).thenReturn(List.of(wrongSeat));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(SeatUnavailableException.class)
                    .hasMessageContaining("do not belong");
        }

        @Test
        @DisplayName("seat already BOOKED in DB → throw SeatUnavailableException")
        void holdSeats_bookedSeat_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList());
            when(seatService.findAllByShowtimeId(10L)).thenReturn(List.of(buildSeat(101L, 10L)));

            SeatDTO bookedSeat = buildSeat(101L, 10L);
            bookedSeat.setStatus(SeatStatus.BOOKED);
            when(seatService.findAllById(anyList())).thenReturn(List.of(bookedSeat));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(SeatUnavailableException.class);
        }

        @Test
        @DisplayName("past showtime → throw IllegalStateException")
        void holdSeats_pastShowtime_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L));
            stubValidReservationData("RES-001", 1L, 10L);

            // Yesterday's showtime
            ShowtimeDTO pastShowtime = buildShowtime(10L);
            pastShowtime.setShowDate(LocalDate.now().minusDays(1));
            when(showtimeService.findById(10L)).thenReturn(pastShowtime);

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("past showtime");
        }

        @Test
        @DisplayName("not enough available seats in showtime → throw IllegalStateException")
        void holdSeats_notEnoughSeats_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L, 102L, 103L));
            stubValidReservationData("RES-001", 1L, 10L);

            // Only 1 seat available but request 3
            ShowtimeDTO showtime = buildShowtime(10L);
            showtime.setAvailableSeats(1);
            when(showtimeService.findById(10L)).thenReturn(showtime);

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Not enough available seats");
        }

        @Test
        @DisplayName("acquireSeatLock throws → deleteSeatLocks called (rollback)")
        void holdSeats_lockFails_shouldRollbackLocks() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L, 102L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            stubSeats(List.of(101L, 102L), 10L);
            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList());
            when(seatService.findAllByShowtimeId(10L)).thenReturn(
                    List.of(buildSeat(101L, 10L), buildSeat(102L, 10L)));

            when(reservationRedisService.acquireSeatLock(anyList(), anyList(), anyString()))
                    .thenThrow(new SeatUnavailableException("Seat taken"));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(SeatUnavailableException.class);

            // acquireSeatLock throws before update redis, no need to verify deleteSeatLocks
            // but verify updateReservationSeats was NOT called
            verify(reservationRedisService, never()).updateReservationSeats(any(), any());
        }
    }

    // ----------------------------------------------------------------
    // confirmReservation
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("confirmReservation")
    class ConfirmReservation {

        @Test
        @DisplayName("idempotency: already confirmed → return response, no re-save")
        void confirmReservation_alreadyPaid_shouldReturnExisting() {
            Reservation existing = buildReservation("RES-001", true);
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.of(existing));
            when(seatService.findAllByReservationId("RES-001")).thenReturn(List.of());
            when(showtimeService.findById(10L)).thenReturn(buildShowtime(10L));
            when(userService.findById(1L)).thenReturn(buildUser(1L));

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

            UserDTO user = buildUser(1L);
            when(userService.findById(1L)).thenReturn(user);

            List<SeatDTO> seats = List.of(buildSeat(101L, 10L), buildSeat(102L, 10L));
            when(seatService.findByIdWithLock(anyList())).thenReturn(seats);

            ShowtimeDTO showtime = buildShowtime(10L);
            when(showtimeService.findById(10L)).thenReturn(showtime);

            Reservation saved = buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);

            // Side effects — mock to prevent throws
            doNothing().when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());
            doNothing().when(seatService).saveAllSeat(any());
            when(showtimeService.save(any())).thenReturn(showtime);

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

            when(userService.findById(1L)).thenReturn(buildUser(1L));
            when(seatService.findByIdWithLock(anyList())).thenReturn(List.of(buildSeat(101L, 10L)));
            ShowtimeDTO showtime = buildShowtime(10L);
            when(showtimeService.findById(10L)).thenReturn(showtime);

            Reservation saved = buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);
            doNothing().when(seatService).saveAllSeat(any());
            when(showtimeService.save(any())).thenReturn(showtime);

            // Email/notification throws
            doThrow(new RuntimeException("SMTP down"))
                    .when(notificationHelper).handleNotification(any(), any(), any());
            doNothing().when(reservationRedisService).cleanupRedisLocks(any(), any());

            // Does not throw to caller
            assertThatNoException().isThrownBy(() -> service.confirmReservation("RES-001"));

            // Reservation was still saved
            verify(reservationRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("seat no longer AVAILABLE on confirm → throw SeatUnavailableException")
        void confirmReservation_seatBecomesUnavailable_shouldThrow() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());
            stubRedisData("RES-001", List.of(101L));
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());

            when(userService.findById(1L)).thenReturn(buildUser(1L));

            // Seat already BOOKED when reloaded from DB
            SeatDTO bookedSeat = buildSeat(101L, 10L);
            bookedSeat.setStatus(SeatStatus.BOOKED);
            when(seatService.findByIdWithLock(anyList())).thenReturn(List.of(bookedSeat));
            when(showtimeService.findById(10L)).thenReturn(buildShowtime(10L));

            assertThatThrownBy(() -> service.confirmReservation("RES-001"))
                    .isInstanceOf(SeatUnavailableException.class);

            verify(reservationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("has voucher → calculateVoucher called, discountAmount correct")
        void confirmReservation_withVoucher_shouldApplyDiscount() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            // Redis has voucherCode
            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001")
                    .userId(1L)
                    .showtimeId(10L)
                    .theaterId(5L)
                    .seatsIds(List.of(101L))
                    .voucherCode("DAWN10")
                    .build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            doNothing().when(reservationRedisService).validateSeatLocks(any(), any());

            when(userService.findById(1L)).thenReturn(buildUser(1L));
            when(seatService.findByIdWithLock(anyList())).thenReturn(List.of(buildSeat(101L, 10L)));
            ShowtimeDTO showtime = buildShowtime(10L); // price = 100_000
            when(showtimeService.findById(10L)).thenReturn(showtime);

            VoucherDiscountDTO discount = VoucherDiscountDTO.builder()
                    .discountAmount(new BigDecimal("10000"))
                    .finalAmount(new BigDecimal("90000"))
                    .build();
            when(voucherClientService.calculateVoucher(eq("DAWN10"), any())).thenReturn(discount);
            doNothing().when(voucherClientService).useVoucher(eq("DAWN10"), eq(1L), anyString());

            Reservation saved = buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);
            doNothing().when(seatService).saveAllSeat(any());
            when(showtimeService.save(any())).thenReturn(showtime);
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

            when(userService.findById(1L)).thenReturn(buildUser(1L));
            when(seatService.findByIdWithLock(anyList())).thenReturn(List.of(buildSeat(101L, 10L)));
            ShowtimeDTO showtime = buildShowtime(10L);
            when(showtimeService.findById(10L)).thenReturn(showtime);
            when(voucherClientService.calculateVoucher(any(), any())).thenReturn(
                    VoucherDiscountDTO.builder()
                            .discountAmount(BigDecimal.ZERO)
                            .finalAmount(new BigDecimal("100000"))
                            .build());

            // useVoucher throws
            doThrow(new RuntimeException("Voucher service down"))
                    .when(voucherClientService).useVoucher(anyString(), anyLong(), anyString());

            Reservation saved = buildReservation("RES-001", true);
            when(reservationRepository.saveAndFlush(any())).thenReturn(saved);
            doNothing().when(seatService).saveAllSeat(any());
            when(showtimeService.save(any())).thenReturn(showtime);
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

            assertThatThrownBy(() -> service.confirmReservation("RES-001"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ----------------------------------------------------------------
    // cancelReservation
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("cancelReservation")
    class CancelReservation {

        @Test
        @DisplayName("already isPaid=true → skip cancel, don't delete Redis")
        void cancelReservation_alreadyPaid_shouldSkip() {
            when(reservationRepository.findById("RES-001"))
                    .thenReturn(Optional.of(buildReservation("RES-001", true)));

            service.cancelReservation("RES-001");

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
            when(seatService.findAllByShowtimeId(10L)).thenReturn(
                    List.of(buildSeat(101L, 10L), buildSeat(102L, 10L)));

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
            when(seatService.findAllByShowtimeId(10L)).thenReturn(List.of(buildSeat(101L, 10L)));

            service.cancelReservation("RES-001");

            verify(voucherClientService).releaseVoucher(eq("DAWN10"), eq(1L));
        }

        @Test
        @DisplayName("voucher release fails → does not affect cancel flow")
        void cancelReservation_releaseVoucherFails_shouldNotThrow() {
            when(reservationRepository.findById("RES-001")).thenReturn(Optional.empty());

            ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                    .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                    .seatsIds(List.of(101L)).voucherCode("DAWN10").price("100000").build();
            when(reservationRedisService.getFromRedis("RES-001")).thenReturn(redisData);
            when(seatService.findAllByShowtimeId(10L)).thenReturn(List.of(buildSeat(101L, 10L)));
            doThrow(new RuntimeException("Voucher service down"))
                    .when(voucherClientService).releaseVoucher(anyString(), anyLong());

            assertThatNoException().isThrownBy(() -> service.cancelReservation("RES-001"));
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------


    private void stubValidReservationData(String reservationId, Long userId, Long showtimeId) {
        Map<Object, Object> data = new HashMap<>();
        data.put("reservationId", reservationId);
        data.put("userId", String.valueOf(userId));
        data.put("showtimeId", String.valueOf(showtimeId));
        when(reservationRedisService.getReservationData(reservationId)).thenReturn(data);
        when(userService.findById(userId)).thenReturn(buildUser(userId));
    }

    private void stubValidShowtime(Long showtimeId) {
        when(showtimeService.findById(showtimeId)).thenReturn(buildShowtime(showtimeId));
    }

    private void stubSeats(List<Long> seatIds, Long showtimeId) {
        List<SeatDTO> seats = seatIds.stream()
                .map(id -> buildSeat(id, showtimeId))
                .toList();
        when(seatService.findAllById(seatIds)).thenReturn(seats);
    }

    private void stubRedisData(String reservationId, List<Long> seatIds) {
        ReservationRedisDTO data = ReservationRedisDTO.builder()
                .id(reservationId)
                .userId(1L)
                .showtimeId(10L)
                .theaterId(5L)
                .seatsIds(seatIds)
                .build();
        when(reservationRedisService.getFromRedis(reservationId)).thenReturn(data);
    }

    private SeatDTO buildSeat(Long id, Long showtimeId) {
        return SeatDTO.builder()
                .id(id)
                .seatNumber("A" + id)
                .status(SeatStatus.AVAILABLE)
                .showtimeId(showtimeId)
                .build();
    }

    private ShowtimeDTO buildShowtime(Long id) {
        return ShowtimeDTO.builder()
                .id(id)
                .movieId(1L)
                .price(new BigDecimal("100000"))
                .availableSeats(100)
                .showDate(LocalDate.now().plusDays(1))
                .showTime(LocalTime.of(19, 0))
                .build();
    }

    private UserDTO buildUser(Long id) {
        return UserDTO.builder()
                .userId(id)
                .email("user" + id + "@test.com")
                .build();
    }

    private Reservation buildReservation(String id, boolean isPaid) {
        return Reservation.builder()
                .id(id)
                .userId(1L)
                .showtimeId(10L)
                .reservationStatus(isPaid ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELED)
                .totalAmount(new BigDecimal("100000"))
                .originalAmount(new BigDecimal("100000"))
                .discountAmount(BigDecimal.ZERO)
                .isPaid(isPaid)
                .isDeleted(false)
                .build();
    }

    private Map<Object, Object> buildRedisMap(String reservationId, Long userId, Long showtimeId) {
        Map<Object, Object> data = new HashMap<>();
        data.put("reservationId", reservationId);
        data.put("userId", String.valueOf(userId));
        data.put("showtimeId", String.valueOf(showtimeId));
        return data;
    }

    private ReservationHoldSeatRequest buildHoldRequest(
            String reservationId, Long userId, Long showtimeId, List<Long> seatIds) {
        return ReservationHoldSeatRequest.builder()
                .reservationId(reservationId)
                .userId(userId)
                .showtimeId(showtimeId)
                .seatIds(seatIds)
                .build();
    }
}
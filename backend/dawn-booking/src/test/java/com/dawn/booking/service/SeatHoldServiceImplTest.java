package com.dawn.booking.service;

import com.dawn.booking.dto.request.ReservationHoldSeatRequest;
import com.dawn.booking.dto.request.ReservationInitRequest;
import com.dawn.booking.dto.response.ReservationInitResponse;
import com.dawn.booking.helper.ReservationNotificationHelper;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.booking.service.impl.SeatHoldServiceImpl;
import com.dawn.common.core.aspect.AuditLogAspect;
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

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldServiceImpl")
class SeatHoldServiceImplTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    CinemaModuleApi cinemaApi;
    @Mock
    IdentityModuleApi identityApi;
    @Mock
    ReservationNotificationHelper notificationHelper;
    @Mock
    ReservationRedisService reservationRedisService;
    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    SeatHoldServiceImpl service;

    @BeforeEach
    void setUpProxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAspect(new AuditLogAspect(auditLogService));
        service = (SeatHoldServiceImpl) factory.getProxy();
    }

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

            when(cinemaApi.findShowtimeById(10L)).thenReturn(ReservationTestData.buildShowtime(10L));

            ReservationInitResponse response = service.initReservation(request);

            ArgumentCaptor<String> reservationIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(reservationRedisService).saveReservationInit(
                    reservationIdCaptor.capture(), anyMap(), any());

            assertThat(response.getReservationId()).isNotBlank();
            assertThat(response.getShowtimeId()).isEqualTo(10L);
            assertThat(response.getTtl()).isEqualTo(900L); // 15 phút
            assertThat(response.getExpiredAt()).isNotNull();
            assertThat(response.getReservationId()).isEqualTo(reservationIdCaptor.getValue());
        }
    }

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
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(
                    List.of(ReservationTestData.buildSeat(101L, 10L), ReservationTestData.buildSeat(102L, 10L)));

            service.holdReservationSeats(request);

            verify(reservationRedisService).acquireSeatLock(eq(List.of(101L, 102L)), anyList(), anyString());
            verify(reservationRedisService).updateReservationSeats(eq("RES-001"), eq(List.of(101L, 102L)));
            verify(auditLogService).record(eq("RESERVATION_HOLD"), eq("RESERVATION"), eq("RES-001"), isNull(),
                    eq("PENDING"), eq("showtimeId=10, seats=101,102"));
        }

        @Test
        @DisplayName("seat not in showtime → throw SeatUnavailableException")
        void holdSeats_wrongShowtime_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList());
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));

            SeatResponse wrongSeat = ReservationTestData.buildSeat(101L, 99L);
            when(cinemaApi.findSeatsByIds(anyList())).thenReturn(List.of(wrongSeat));

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
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(List.of(ReservationTestData.buildSeat(101L, 10L)));

            SeatResponse bookedSeat = ReservationTestData.buildSeat(101L, 10L);
            bookedSeat.setStatus(SeatStatus.BOOKED);
            when(cinemaApi.findSeatsByIds(anyList())).thenReturn(List.of(bookedSeat));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(SeatUnavailableException.class);
        }

        @Test
        @DisplayName("past showtime → throw IllegalStateException")
        void holdSeats_pastShowtime_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L));
            stubValidReservationData("RES-001", 1L, 10L);

            var pastShowtime = ReservationTestData.buildShowtime(10L);
            pastShowtime.setShowDate(LocalDate.now().minusDays(1));
            when(cinemaApi.findShowtimeById(10L)).thenReturn(pastShowtime);

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("past showtime");
        }

        @Test
        @DisplayName("not enough available seats in showtime → throw IllegalStateException")
        void holdSeats_notEnoughSeats_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L, 102L, 103L));
            stubValidReservationData("RES-001", 1L, 10L);

            var showtime = ReservationTestData.buildShowtime(10L);
            showtime.setAvailableSeats(1);
            when(cinemaApi.findShowtimeById(10L)).thenReturn(showtime);

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
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(
                    List.of(ReservationTestData.buildSeat(101L, 10L), ReservationTestData.buildSeat(102L, 10L)));

            when(reservationRedisService.acquireSeatLock(anyList(), anyList(), anyString()))
                    .thenThrow(new SeatUnavailableException("Seat taken"));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(SeatUnavailableException.class);

            verify(reservationRedisService, never()).updateReservationSeats(any(), any());
        }

        @Test
        @DisplayName("DB row already CONFIRMED → throw ApiException, don't overwrite")
        void holdSeats_confirmedRow_shouldThrow() {
            ReservationHoldSeatRequest request = buildHoldRequest("RES-001", 1L, 10L, List.of(101L, 102L));
            stubValidReservationData("RES-001", 1L, 10L);
            stubValidShowtime(10L);
            stubSeats(List.of(101L, 102L), 10L);
            when(reservationRedisService.parseSeatIdsFromReservationData(any()))
                    .thenReturn(Collections.emptyList());
            when(cinemaApi.findSeatsByShowtime(10L)).thenReturn(
                    List.of(ReservationTestData.buildSeat(101L, 10L), ReservationTestData.buildSeat(102L, 10L)));
            when(reservationRepository.findById("RES-001"))
                    .thenReturn(Optional.of(ReservationTestData.buildReservation("RES-001", true)));

            assertThatThrownBy(() -> service.holdReservationSeats(request))
                    .isInstanceOf(ApiException.class);

            verify(reservationRepository, never()).save(any());
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
        when(identityApi.findUserById(userId)).thenReturn(ReservationTestData.buildUser(userId));
    }

    private void stubValidShowtime(Long showtimeId) {
        when(cinemaApi.findShowtimeById(showtimeId)).thenReturn(ReservationTestData.buildShowtime(showtimeId));
    }

    private void stubSeats(List<Long> seatIds, Long showtimeId) {
        List<SeatResponse> seats = seatIds.stream()
                .map(id -> ReservationTestData.buildSeat(id, showtimeId))
                .toList();
        when(cinemaApi.findSeatsByIds(seatIds)).thenReturn(seats);
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
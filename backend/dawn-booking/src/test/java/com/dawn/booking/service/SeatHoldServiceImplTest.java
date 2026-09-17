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
import com.dawn.common.infra.redis.service.VelocityGuard;
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
    @Mock
    VelocityGuard velocityGuard;

    @InjectMocks
    SeatHoldServiceImpl service;

    @BeforeEach
    void setUpProxy() {
        lenient().when(velocityGuard.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAspect(new AuditLogAspect(auditLogService,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new com.dawn.common.core.aspect.AuditMessageBuilder(new com.fasterxml.jackson.databind.ObjectMapper()),
                null));
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

            assertThat(response.getReservationCode()).isNotBlank();
            assertThat(response.getShowtimeId()).isEqualTo(10L);
            assertThat(response.getTtl()).isEqualTo(900L); // 15 minutes
            assertThat(response.getExpiredAt()).isNotNull();
            assertThat(response.getReservationCode()).isEqualTo(reservationIdCaptor.getValue());
        }

        @Test
        @DisplayName("velocity exceeded → 429 Too Many Requests")
        void initReservation_velocityExceeded_shouldThrow429() {
            ReservationInitRequest request = ReservationInitRequest.builder()
                    .userId(1L)
                    .showtimeId(10L)
                    .theaterId(5L)
                    .build();

            when(velocityGuard.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.initReservation(request))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Too many hold attempts")
                    .matches(e -> ((ApiException) e).getStatus() == org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            verify(reservationRedisService, never()).saveReservationInit(any(), any(), any());
        }

        @Test
        @DisplayName("cùng idempotencyKey → replay reservation code cũ, KHÔNG tạo mới")
        void initReservation_sameIdempotencyKey_shouldReplayExistingCode() {
            ReservationInitRequest request = ReservationInitRequest.builder()
                    .userId(1L)
                    .showtimeId(10L)
                    .theaterId(5L)
                    .idempotencyKey("IDEM-123")
                    .build();

            when(reservationRepository.findByIdempotencyKey("IDEM-123"))
                    .thenReturn(Optional.of(ReservationTestData.buildReservation("CP-REPLAY", false)));
            when(reservationRedisService.getReservationTtl("CP-REPLAY")).thenReturn(500L);

            ReservationInitResponse response = service.initReservation(request);

            assertThat(response.getReservationCode()).isEqualTo("CP-REPLAY");
            assertThat(response.getTtl()).isEqualTo(500L);
            verify(reservationRedisService, never()).saveReservationInit(any(), any(), any());
        }

        @Test
        @DisplayName("cùng idempotencyKey nhưng index hết hạn → tạo mới")
        void initReservation_staleIdempotencyIndex_shouldGenerateNew() {
            ReservationInitRequest request = ReservationInitRequest.builder()
                    .userId(1L)
                    .showtimeId(10L)
                    .theaterId(5L)
                    .idempotencyKey("IDEM-EXPIRED")
                    .build();

            when(reservationRepository.findByIdempotencyKey("IDEM-EXPIRED"))
                    .thenReturn(Optional.empty());
            when(reservationRedisService.getReservationIdByIdempotencyKey("IDEM-EXPIRED"))
                    .thenReturn("CP-OLD");
            when(reservationRedisService.getReservationTtl("CP-OLD")).thenReturn(-1L);
            when(cinemaApi.findShowtimeById(10L)).thenReturn(ReservationTestData.buildShowtime(10L));

            ReservationInitResponse response = service.initReservation(request);

            assertThat(response.getReservationCode()).isNotEqualTo("CP-OLD").isNotBlank();
            assertThat(response.getTtl()).isEqualTo(900L);
        }
    }

    @Nested
    @DisplayName("holdSeats")
    class HoldSeats {

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

            service.holdSeats(request);

            verify(reservationRedisService).acquireSeatLock(eq(List.of(101L, 102L)), anyList(), anyString());
            verify(reservationRedisService).updateReservationSeats(eq("RES-001"), eq(List.of(101L, 102L)));
            verify(auditLogService).record(eq("RESERVATION_HOLD"), eq("RESERVATION"), anyString(), isNull(),
                    isNull(), eq("PENDING"), anyString(), eq("SUCCESS"), anyString(), isNull(), isNull());
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

            assertThatThrownBy(() -> service.holdSeats(request))
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

            assertThatThrownBy(() -> service.holdSeats(request))
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

            assertThatThrownBy(() -> service.holdSeats(request))
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

            assertThatThrownBy(() -> service.holdSeats(request))
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

            assertThatThrownBy(() -> service.holdSeats(request))
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
            when(reservationRepository.findByReservationCode("RES-001"))
                    .thenReturn(Optional.of(ReservationTestData.buildReservation("RES-001", true)));

            assertThatThrownBy(() -> service.holdSeats(request))
                    .isInstanceOf(ApiException.class);

            verify(reservationRepository, never()).save(any());
        }
    }


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
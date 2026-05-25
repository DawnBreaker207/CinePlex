package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.SeatDTO;
import com.dawn.common.core.constant.SeatStatus;
import com.dawn.common.core.exception.wrapper.RedisStorageException;
import com.dawn.common.core.exception.wrapper.ReservationExpiredException;
import com.dawn.common.core.exception.wrapper.SeatUnavailableException;
import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.common.infra.redis.service.RedisPublisher;
import com.dawn.common.infra.redis.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationRedisService")
class ReservationRedisServiceTest {

    @Mock
    RedisService redisService;
    @Mock
    RedisPublisher redisPublisher;

    // ObjectMapper dùng thật — không mock, vì cần serialize/deserialize JSON thật
    ObjectMapper objectMapper = new ObjectMapper();

    ReservationRedisService service;

    @BeforeEach
    void setUp() {
        service = new ReservationRedisService(redisService, redisPublisher, objectMapper);
    }

    // ----------------------------------------------------------------
    // getFromRedis
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("getFromRedis")
    class GetFromRedis {

        @Test
        @DisplayName("data tồn tại → parse đúng ReservationRedisDTO")
        void getFromRedis_validData_shouldParseCorrectly() {
            Map<Object, Object> data = new HashMap<>();
            data.put("userId", "1");
            data.put("showtimeId", "10");
            data.put("theaterId", "5");
            data.put("voucherCode", "DAWN10");
            data.put("seatIds", "[101, 102]");
            data.put("tempFinalAmount", "200000");

            when(redisService.getHash(RedisKeyHelper.reservationHoldKey("RES-001")))
                    .thenReturn(data);

            ReservationRedisDTO result = service.getFromRedis("RES-001");

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getShowtimeId()).isEqualTo(10L);
            assertThat(result.getTheaterId()).isEqualTo(5L);
            assertThat(result.getVoucherCode()).isEqualTo("DAWN10");
            assertThat(result.getSeatsIds()).containsExactly(101L, 102L);
        }

        @Test
        @DisplayName("data null/empty → throw ReservationExpiredException")
        void getFromRedis_emptyData_shouldThrow() {
            when(redisService.getHash(any())).thenReturn(Collections.emptyMap());

            assertThatThrownBy(() -> service.getFromRedis("RES-001"))
                    .isInstanceOf(ReservationExpiredException.class);
        }

        @Test
        @DisplayName("seatIds null trong data → trả về list rỗng")
        void getFromRedis_nullSeatIds_shouldReturnEmptyList() {
            Map<Object, Object> data = new HashMap<>();
            data.put("userId", "1");
            data.put("showtimeId", "10");
            data.put("theaterId", "5");
            data.put("voucherCode", null);
            data.put("seatIds", null);

            when(redisService.getHash(any())).thenReturn(data);

            ReservationRedisDTO result = service.getFromRedis("RES-001");

            assertThat(result.getSeatsIds()).isEmpty();
        }

        @Test
        @DisplayName("userId không phải số → throw RedisStorageException")
        void getFromRedis_invalidUserId_shouldThrow() {
            Map<Object, Object> data = new HashMap<>();
            data.put("userId", "not-a-number");
            data.put("showtimeId", "10");
            data.put("theaterId", "5");

            when(redisService.getHash(any())).thenReturn(data);

            assertThatThrownBy(() -> service.getFromRedis("RES-001"))
                    .isInstanceOf(RedisStorageException.class)
                    .hasMessageContaining("userId");
        }
    }

    // ----------------------------------------------------------------
    // acquireSeatLock
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("acquireSeatLock")
    class AcquireSeatLock {

        @Test
        @DisplayName("lock thành công → trả về danh sách seatIds")
        void acquireSeatLock_success_shouldReturnSeatIds() {
            List<Long> seatIds = List.of(1L, 2L);
            List<SeatDTO> seats = buildSeats(seatIds);

            when(redisService.lockMulti(anyList(), anyString(), any()))
                    .thenReturn(List.of(1L));

            List<Long> result = service.acquireSeatLock(seatIds, seats, "reservation:data:RES-001");

            assertThat(result).isEqualTo(seatIds);
        }

        @Test
        @DisplayName("lock fail — ghế bị chiếm → throw SeatUnavailableException với seat number")
        void acquireSeatLock_seatTaken_shouldThrowWithSeatNumber() {
            List<Long> seatIds = List.of(1L, 2L);
            List<SeatDTO> seats = buildSeats(seatIds);
            // seat:locked:2 bị chiếm
            when(redisService.lockMulti(anyList(), anyString(), any()))
                    .thenReturn(Arrays.asList(0L, "seat:locked:2", "reservation:data:OTHER"));

            assertThatThrownBy(() ->
                    service.acquireSeatLock(seatIds, seats, "reservation:data:RES-001"))
                    .isInstanceOf(SeatUnavailableException.class)
                    .hasMessageContaining("A2"); // seat number từ buildSeats
        }

        @Test
        @DisplayName("lockMulti trả về null → throw SeatUnavailableException")
        void acquireSeatLock_nullResult_shouldThrow() {
            List<Long> seatIds = List.of(1L);
            List<SeatDTO> seats = buildSeats(seatIds);

            when(redisService.lockMulti(anyList(), anyString(), any())).thenReturn(null);

            assertThatThrownBy(() ->
                    service.acquireSeatLock(seatIds, seats, "reservation:data:RES-001"))
                    .isInstanceOf(SeatUnavailableException.class);
        }
    }

    // ----------------------------------------------------------------
    // validateSeatLocks
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("validateSeatLocks")
    class ValidateSeatLocks {

        @Test
        @DisplayName("tất cả lock còn valid và đúng owner → không throw")
        void validateSeatLocks_allValid_shouldNotThrow() {
            String reservationId = "RES-001";
            String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);
            List<Long> seatIds = List.of(1L, 2L);

            when(redisService.multiGet(anyList()))
                    .thenReturn(Arrays.asList(redisKey, redisKey));

            assertThatNoException().isThrownBy(
                    () -> service.validateSeatLocks(reservationId, seatIds));
        }

        @Test
        @DisplayName("một lock đã expire (null) → throw SeatUnavailableException")
        void validateSeatLocks_expiredLock_shouldThrow() {
            String reservationId = "RES-001";
            List<Long> seatIds = List.of(1L, 2L);

            // seat 1 expired
            when(redisService.multiGet(anyList()))
                    .thenReturn(Arrays.asList(null, RedisKeyHelper.reservationHoldKey(reservationId)));

            assertThatThrownBy(() -> service.validateSeatLocks(reservationId, seatIds))
                    .isInstanceOf(SeatUnavailableException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("lock bị chiếm bởi owner khác → throw SeatUnavailableException")
        void validateSeatLocks_stolenLock_shouldThrow() {
            String reservationId = "RES-001";
            List<Long> seatIds = List.of(1L, 2L);

            when(redisService.multiGet(anyList()))
                    .thenReturn(Arrays.asList(
                            RedisKeyHelper.reservationHoldKey(reservationId),
                            "reservation:data:OTHER-RES" // stolen
                    ));

            assertThatThrownBy(() -> service.validateSeatLocks(reservationId, seatIds))
                    .isInstanceOf(SeatUnavailableException.class)
                    .hasMessageContaining("taken");
        }
    }

    // ----------------------------------------------------------------
    // deleteSeatLockIfOwner / cleanupRedisLocks
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("cleanupRedisLocks")
    class CleanupRedisLocks {

        @Test
        @DisplayName("cleanup đúng owner → xóa tất cả lock và reservation key")
        void cleanupRedisLocks_shouldDeleteAllLocksAndReservationKey() {
            String reservationId = "RES-001";
            List<SeatDTO> seats = buildSeats(List.of(1L, 2L));
            String redisKey = RedisKeyHelper.reservationHoldKey(reservationId);

            when(redisService.releaseLock(any(), eq(redisKey))).thenReturn(true);

            service.cleanupRedisLocks(reservationId, seats);

            verify(redisService, times(2)).releaseLock(any(), eq(redisKey));
            verify(redisService).delete(RedisKeyHelper.reservationHoldKey(reservationId));
        }
    }

    // ----------------------------------------------------------------
    // parseSeatIdsFromReservationData
    // ----------------------------------------------------------------

    @Test
    @DisplayName("parseSeatIds: JSON hợp lệ → trả về list đúng")
    void parseSeatIds_validJson_shouldReturnList() {
        Map<Object, Object> data = Map.of("seatIds", "[1, 2, 3]");

        List<Long> result = service.parseSeatIdsFromReservationData(data);

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("parseSeatIds: seatIds rỗng → trả về list rỗng")
    void parseSeatIds_emptyJson_shouldReturnEmptyList() {
        Map<Object, Object> data = new HashMap<>();
        data.put("seatIds", "");

        List<Long> result = service.parseSeatIdsFromReservationData(data);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseSeatIds: JSON invalid → throw RedisStorageException")
    void parseSeatIds_invalidJson_shouldThrow() {
        Map<Object, Object> data = Map.of("seatIds", "not-json");

        assertThatThrownBy(() -> service.parseSeatIdsFromReservationData(data))
                .isInstanceOf(RedisStorageException.class);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private List<SeatDTO> buildSeats(List<Long> seatIds) {
        List<SeatDTO> seats = new ArrayList<>();
        for (Long id : seatIds) {
            SeatDTO seat = SeatDTO.builder()
                    .id(id)
                    .seatNumber("A" + id)
                    .status(SeatStatus.AVAILABLE)
                    .showtimeId(10L)
                    .build();
            seats.add(seat);
        }
        return seats;
    }
}
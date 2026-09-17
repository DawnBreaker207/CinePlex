package com.dawn.booking.service;

import com.dawn.booking.dto.response.ReservationRedisDTO;
import com.dawn.booking.dto.response.VoucherDiscountDTO;
import com.dawn.booking.model.Reservation;
import com.dawn.booking.repository.ReservationRepository;
import com.dawn.booking.service.impl.VoucherApplicationServiceImpl;
import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.cinema.api.CinemaModuleApi;
import com.dawn.cinema.dto.response.SeatResponse;
import com.dawn.cinema.dto.response.ShowtimeResponse;
import com.dawn.identity.api.IdentityModuleApi;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.common.core.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherApplicationServiceImpl")
class VoucherApplicationServiceImplTest {

    @Mock
    CinemaModuleApi cinemaApi;
    @Mock
    ReservationRedisService reservationRedisService;
    @Mock
    CatalogModuleApi catalogApi;
    @Mock
    ReservationRepository reservationRepository;

    @InjectMocks
    VoucherApplicationServiceImpl service;

    @Test
    @DisplayName("applyVoucher → total = sum of seat prices, voucher saved to Redis, discount returned")
    void applyVoucher_success_shouldCalculateAndPersist() {
        ReservationRedisDTO redisData = ReservationRedisDTO.builder()
                .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                .seatsIds(List.of(101L, 102L)).build();
        when(reservationRedisService.getReservationSession("RES-001")).thenReturn(redisData);
        when(cinemaApi.findShowtimeById(10L)).thenReturn(ReservationTestData.buildShowtime(10L)); // price 100_000
        when(cinemaApi.findSeatsByIds(List.of(101L, 102L))).thenReturn(List.of(
                ReservationTestData.buildSeat(101L, 10L),
                ReservationTestData.buildSeat(102L, 10L)));
        when(catalogApi.calculateVoucher(eq("DAWN10"), eq(new BigDecimal("200000"))))
                .thenReturn(VoucherCalculation.builder()
                        .code("DAWN10")
                        .originalAmount(new BigDecimal("200000"))
                        .discountAmount(new BigDecimal("20000"))
                        .finalAmount(new BigDecimal("180000"))
                        .build());
        when(reservationRepository.findByReservationCode("RES-001")).thenReturn(Optional.empty());

        VoucherDiscountDTO result = service.applyVoucher("RES-001", "DAWN10");

        assertThat(result.getCode()).isEqualTo("DAWN10");
        assertThat(result.getOriginalAmount()).isEqualByComparingTo("200000");
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("20000");
        assertThat(result.getFinalAmount()).isEqualByComparingTo("180000");

        verify(reservationRedisService).saveVoucher(argThat(d -> "DAWN10".equals(d.getVoucherCode())));
    }

    @Test
    @DisplayName("applyVoucher → Redis session missing → ApiException, no NPE")
    void applyVoucher_noRedis_shouldThrowNotFound() {
        when(reservationRedisService.getReservationSession("RES-001")).thenReturn(null);

        assertThatThrownBy(() -> service.applyVoucher("RES-001", "DAWN10"))
                .isInstanceOf(ApiException.class);

        verify(catalogApi, never()).calculateVoucher(anyString(), any());
    }

    @Test
    @DisplayName("releaseVoucher → catalogApi.releaseVoucher with code + userId")
    void releaseVoucher_withCode_shouldCallCatalog() {
        ReservationRedisDTO cachedData = ReservationRedisDTO.builder()
                .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                .seatsIds(List.of(101L)).voucherCode("DAWN10").build();

        service.releaseVoucher(cachedData, Optional.empty());

        verify(catalogApi).releaseVoucher("DAWN10", 1L);
    }

    @Test
    @DisplayName("releaseVoucher → falls back to DB row when no Redis data")
    void releaseVoucher_noRedis_shouldFallbackToDbRow() {
        Reservation dbRow = ReservationTestData.buildReservation("RES-001", false);
        dbRow.setVoucherCode("DBV10");

        service.releaseVoucher(null, Optional.of(dbRow));

        verify(catalogApi).releaseVoucher("DBV10", 1L);
    }

    @Test
    @DisplayName("releaseVoucher → no code, no call")
    void releaseVoucher_noCode_shouldNotCallCatalog() {
        ReservationRedisDTO cachedData = ReservationRedisDTO.builder()
                .id("RES-001").userId(1L).showtimeId(10L).theaterId(5L)
                .seatsIds(List.of(101L)).build();

        service.releaseVoucher(cachedData, Optional.empty());

        verify(catalogApi, never()).releaseVoucher(anyString(), anyLong());
    }
}
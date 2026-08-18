package com.dawn.catalog.internal.impl;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.model.UserVoucher;
import com.dawn.catalog.model.Voucher;
import com.dawn.catalog.repository.UserVoucherRepository;
import com.dawn.catalog.repository.VoucherRepository;
import com.dawn.catalog.internal.VoucherService;
import com.dawn.common.core.constant.UserVoucherStatus;
import com.dawn.common.core.constant.VoucherStatus;
import com.dawn.common.core.exception.wrapper.InvalidRequestException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherServiceImpl")
class VoucherServiceImplTest {

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    UserVoucherRepository userVoucherRepository;

    @InjectMocks
    VoucherServiceImpl service;

    private Voucher createVoucher(VoucherStatus status, DiscountType type, BigDecimal value,
                                   BigDecimal maxDiscount, BigDecimal minOrder,
                                   int total, int used, Instant start, Instant end) {
        return Voucher.builder()
                .id(1L)
                .name("Test Voucher")
                .code("TEST10")
                .status(status)
                .discountType(type)
                .discountValue(value)
                .maxDiscountAmount(maxDiscount)
                .minOrderValue(minOrder)
                .quantityTotal(total)
                .quantityUsed(used)
                .startAt(start)
                .endAt(end)
                .maxPerUser(1)
                .build();
    }

    // ----------------------------------------------------------------
    // findByCode
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("found → return DTO")
        void findByCode_found_shouldReturnDTO() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            var result = service.findByCode("TEST10");

            assertThat(result.getCode()).isEqualTo("TEST10");
            assertThat(result.getDiscountValue()).isEqualByComparingTo(BigDecimal.TEN);
        }

        @Test
        @DisplayName("not found → throw ResourceNotFoundException")
        void findByCode_notFound_shouldThrow() {
            when(voucherRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ----------------------------------------------------------------
    // calculate
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("FIXED discount → correct discount amount")
        void calculate_fixedDiscount_shouldReturnCorrectAmount() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    new BigDecimal("20000"), null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            VoucherCalculation result = service.calculate("TEST10", new BigDecimal("100000"));

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("80000"));
        }

        @Test
        @DisplayName("PERCENT discount → correct % discount")
        void calculate_percentDiscount_shouldReturnCorrectPercent() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.PERCENT,
                    new BigDecimal("10"), new BigDecimal("50000"), BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            VoucherCalculation result = service.calculate("TEST10", new BigDecimal("100000"));

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("90000"));
        }

        @Test
        @DisplayName("PERCENT exceeds max_discount_amount → capped by max")
        void calculate_percentExceedsMax_shouldCapAtMax() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.PERCENT,
                    new BigDecimal("50"), new BigDecimal("30000"), BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            VoucherCalculation result = service.calculate("TEST10", new BigDecimal("100000"));

            assertThat(result.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(result.getFinalAmount()).isEqualByComparingTo(new BigDecimal("70000"));
        }

        @Test
        @DisplayName("min_order_value not met → throw")
        void calculate_belowMinOrder_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    new BigDecimal("10000"), null, new BigDecimal("50000"),
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.calculate("TEST10", new BigDecimal("30000")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("voucher expired → throw")
        void calculate_expired_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.EXPIRED, DiscountType.FIXED,
                    new BigDecimal("10000"), null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.calculate("TEST10", new BigDecimal("50000")))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("voucher out of quantity → throw")
        void calculate_exhausted_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    new BigDecimal("10000"), null, BigDecimal.ZERO,
                    100, 100, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.calculate("TEST10", new BigDecimal("50000")))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ----------------------------------------------------------------
    // useVoucher
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("useVoucher")
    class UseVoucher {

        @Test
        @DisplayName("success → quantityUsed incremented, UserVoucher status = USED")
        void useVoucher_success_shouldUpdateCountAndStatus() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 5, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            UserVoucher uv = UserVoucher.builder()
                    .id(1L).userId(1L).voucherId(1L).code("TEST10")
                    .status(UserVoucherStatus.AVAILABLE)
                    .claimedAt(Instant.now())
                    .expiredAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build();
            when(userVoucherRepository.findByUserIdAndCodeAndStatus(eq(1L), eq("TEST10"), eq(UserVoucherStatus.AVAILABLE)))
                    .thenReturn(Optional.of(uv));
            when(voucherRepository.useVoucher(anyString(), any())).thenReturn(1);

            service.useVoucher("TEST10", 1L, "RES-001");

            verify(voucherRepository).useVoucher(eq("TEST10"), any());
            assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.USED);
            assertThat(uv.getReservationId()).isEqualTo("RES-001");
        }

        @Test
        @DisplayName("quantity already at max → throw")
        void useVoucher_exhausted_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 100, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.useVoucher("TEST10", 1L, "RES-001"))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("voucher not ACTIVE → throw")
        void useVoucher_inactive_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.PAUSED, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.useVoucher("TEST10", 1L, "RES-001"))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ----------------------------------------------------------------
    // releaseVoucher
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("releaseVoucher")
    class ReleaseVoucher {

        @Test
        @DisplayName("success → release count and mark AVAILABLE")
        void releaseVoucher_success_shouldReleaseCount() {
            service.releaseVoucher("TEST10", 1L);

            verify(voucherRepository).releaseVoucher("TEST10");
            verify(userVoucherRepository).updateStatusByUserIdAndCode(1L, "TEST10", UserVoucherStatus.AVAILABLE);
        }
    }

    // ----------------------------------------------------------------
    // claimVoucher
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("claimVoucher")
    class ClaimVoucher {

        @Test
        @DisplayName("success → create UserVoucher AVAILABLE")
        void claimVoucher_success_shouldCreateUserVoucher() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 10, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));
            when(userVoucherRepository.countByUserIdAndVoucherIdAndStatus(1L, 1L, UserVoucherStatus.AVAILABLE))
                    .thenReturn(0L);
            when(voucherRepository.useVoucher(anyString(), any())).thenReturn(1);

            var result = service.claimVoucher("TEST10", 1L);

            assertThat(result.getCode()).isEqualTo("TEST10");
            assertThat(result.getStatus()).isEqualTo(UserVoucherStatus.AVAILABLE);
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(userVoucherRepository).save(any(UserVoucher.class));
        }

        @Test
        @DisplayName("already reached max_per_user → throw")
        void claimVoucher_exceedsMaxPerUser_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 10, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            voucher.setMaxPerUser(2);
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));
            when(userVoucherRepository.countByUserIdAndVoucherIdAndStatus(1L, 1L, UserVoucherStatus.AVAILABLE))
                    .thenReturn(2L);

            assertThatThrownBy(() -> service.claimVoucher("TEST10", 1L))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("voucher EXPIRED → throw")
        void claimVoucher_expired_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.EXPIRED, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.claimVoucher("TEST10", 1L))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("voucher out of stock → throw")
        void claimVoucher_outOfStock_shouldThrow() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.FIXED,
                    BigDecimal.TEN, null, BigDecimal.ZERO,
                    100, 100, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));
            when(voucherRepository.findByCode("TEST10")).thenReturn(Optional.of(voucher));

            assertThatThrownBy(() -> service.claimVoucher("TEST10", 1L))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ----------------------------------------------------------------
    // getUserVouchers
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("getUserVouchers")
    class GetUserVouchers {

        @Test
        @DisplayName("user has vouchers → return list")
        void getUserVouchers_hasVouchers_shouldReturnList() {
            Voucher voucher = createVoucher(VoucherStatus.ACTIVE, DiscountType.PERCENT,
                    new BigDecimal("10"), null, BigDecimal.ZERO,
                    100, 0, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));

            UserVoucher uv = UserVoucher.builder()
                    .id(1L).userId(1L).voucherId(1L).code("TEST10")
                    .status(UserVoucherStatus.AVAILABLE)
                    .claimedAt(Instant.now())
                    .expiredAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build();

            when(userVoucherRepository.findByUserIdOrderByClaimedAtDesc(1L))
                    .thenReturn(List.of(uv));
            when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

            var results = service.getUserVouchers(1L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().getVoucherName()).isEqualTo("Test Voucher");
            assertThat(results.getFirst().getDiscountType()).isEqualTo(DiscountType.PERCENT);
        }

        @Test
        @DisplayName("user has no vouchers → empty list")
        void getUserVouchers_noVouchers_shouldReturnEmpty() {
            when(userVoucherRepository.findByUserIdOrderByClaimedAtDesc(1L))
                    .thenReturn(List.of());

            var results = service.getUserVouchers(1L);

            assertThat(results).isEmpty();
        }
    }
}

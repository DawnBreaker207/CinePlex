package com.dawn.catalog.dto.response;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.common.core.constant.UserVoucherStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserVoucherResponse {

    private Long id;

    private Long userId;

    private Long voucherId;

    private String code;

    private String voucherName;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderValue;

    private UserVoucherStatus status;

    private Instant claimedAt;

    private Instant usedAt;

    private String reservationId;

    private Instant expiredAt;
}

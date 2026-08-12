package com.dawn.catalog.dto.request;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.constant.VoucherType;
import com.dawn.common.core.constant.VoucherStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class VoucherRequest {

    @NotBlank(message = "Voucher name is required")
    private String name;

    @NotBlank(message = "Voucher code is required")
    private String code;

    @NotNull
    private Integer quantityTotal;

    private VoucherType category;

    private String groupRef;

    private String conditions;

    @NotNull
    private DiscountType discountType;

    @NotNull
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderValue;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    private VoucherStatus status;

    private Integer maxPerUser;
}

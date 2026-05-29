package com.dawn.catalog.dto.request;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.constant.VoucherType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VoucherRequest {
    private String name;

    private String code;

    private Long quantityTotal;

    @Enumerated(EnumType.STRING)
    private VoucherType category;

    private String groupRef;

    private String conditions;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Long discountValue;
    private Long maxDiscountAmount;
    private Long minOrderValue;

    private Instant startAt;
    private Instant endAt;
}

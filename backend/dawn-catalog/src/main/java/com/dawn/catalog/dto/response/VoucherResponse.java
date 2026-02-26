package com.dawn.catalog.dto.response;

import com.dawn.catalog.config.DiscountType;
import com.dawn.catalog.config.VoucherType;
import com.dawn.common.core.dto.response.BaseResponse;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VoucherResponse extends BaseResponse {
    private Long id;

    private String name;

    private String code;

    private Long quantityTotal;

    private Long quantityUsed;

    @Enumerated(EnumType.STRING)
    private VoucherType category;

    private String groupRef;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private Long discountValue;
    private Long maxDiscountAmount;
    private Long minOrderValue;

    private Instant startAt;
    private Instant endAt;

    private Boolean isActive;
}

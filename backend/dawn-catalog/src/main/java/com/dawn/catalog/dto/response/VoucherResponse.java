package com.dawn.catalog.dto.response;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.constant.VoucherType;
import com.dawn.common.core.constant.VoucherStatus;
import com.dawn.common.core.dto.response.BaseResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VoucherResponse extends BaseResponse {
    private Long id;

    private String name;

    private String code;

    private Integer quantityTotal;

    private Integer quantityUsed;

    private VoucherType category;

    private String groupRef;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderValue;

    private Instant startAt;

    private Instant endAt;

    private VoucherStatus status;

    private Integer maxPerUser;

    private String conditions;
}

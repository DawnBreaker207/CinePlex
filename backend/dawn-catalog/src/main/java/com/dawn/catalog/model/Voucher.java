package com.dawn.catalog.model;

import com.dawn.catalog.constant.DiscountType;
import com.dawn.catalog.constant.VoucherType;
import com.dawn.common.core.constant.VoucherStatus;
import com.dawn.common.core.model.AbstractMappedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Voucher extends AbstractMappedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "quantity_total", nullable = false)
    @Builder.Default
    private Integer quantityTotal = 0;

    @Column(name = "quantity_used", nullable = false)
    @Builder.Default
    private Integer quantityUsed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private VoucherType category;

    @Column(name = "group_ref")
    private String groupRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "conditions", columnDefinition = "json")
    private String conditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.SCHEDULED;

    @Column(name = "max_per_user", nullable = false)
    @Builder.Default
    private Integer maxPerUser = 1;

    @Version
    @Column(name = "version")
    private Long version;
}

package com.dawn.catalog.model;

import com.dawn.catalog.config.DiscountType;
import com.dawn.catalog.config.VoucherType;
import com.dawn.common.core.model.AbstractMappedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
    private Long quantityTotal = 0L;

    @Column(name = "quantity_used", nullable = false)
    @Builder.Default
    private Long quantityUsed = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private VoucherType category;

    @Column(name = "group_ref")
    private String groupRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value")
    private Long discountValue;
    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "min_order_value", nullable = false)
    @Builder.Default
    private Long minOrderValue = 0L;

    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;
}

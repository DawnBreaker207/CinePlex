package com.dawn.booking.model;

import com.dawn.common.core.constant.ReservationStatus;
import com.dawn.common.core.model.AbstractMappedEntity;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Hidden
@Entity
@Table(name = "reservation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Reservation extends AbstractMappedEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "voucher_code", nullable = true)
    private String voucherCode;

    @Column(name = "original_amount")
    private BigDecimal originalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}

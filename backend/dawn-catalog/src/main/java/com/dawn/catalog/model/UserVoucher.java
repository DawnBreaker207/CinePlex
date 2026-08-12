package com.dawn.catalog.model;

import com.dawn.common.core.constant.UserVoucherStatus;
import com.dawn.common.core.model.AbstractMappedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "user_voucher")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserVoucher extends AbstractMappedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserVoucherStatus status = UserVoucherStatus.AVAILABLE;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "reservation_id", length = 50)
    private String reservationId;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;
}

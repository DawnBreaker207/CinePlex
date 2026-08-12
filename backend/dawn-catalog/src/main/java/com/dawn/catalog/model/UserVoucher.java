package com.dawn.catalog.model;

import com.dawn.common.core.constant.UserVoucherStatus;
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
@EqualsAndHashCode(callSuper = false)
public class UserVoucher {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserVoucherStatus status = UserVoucherStatus.AVAILABLE;

    @Column(name = "claimed_at", nullable = false)
    @Builder.Default
    private Instant claimedAt = Instant.now();

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "reservation_id", length = 50)
    private String reservationId;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;
}

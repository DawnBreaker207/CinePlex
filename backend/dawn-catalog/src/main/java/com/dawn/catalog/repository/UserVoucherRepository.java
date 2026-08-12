package com.dawn.catalog.repository;

import com.dawn.catalog.model.UserVoucher;
import com.dawn.common.core.constant.UserVoucherStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    List<UserVoucher> findByUserIdOrderByClaimedAtDesc(Long userId);

    Optional<UserVoucher> findByUserIdAndVoucherIdAndStatus(Long userId, Long voucherId, UserVoucherStatus status);

    long countByUserIdAndVoucherIdAndStatus(Long userId, Long voucherId, UserVoucherStatus status);

    @Modifying
    @Query("""
            UPDATE UserVoucher uv SET uv.status = 'USED', uv.usedAt = :now, uv.reservationId = :reservationId
            WHERE uv.userId = :userId AND uv.code = :code AND uv.status = 'AVAILABLE'
            """)
    int markAsUsed(@Param("userId") Long userId, @Param("code") String code,
                   @Param("reservationId") String reservationId, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE UserVoucher uv SET uv.status = 'AVAILABLE', uv.usedAt = NULL, uv.reservationId = NULL
            WHERE uv.userId = :userId AND uv.code = :code AND uv.status = 'USED'
            """)
    int markAsAvailable(@Param("userId") Long userId, @Param("code") String code);

    Optional<UserVoucher> findByUserIdAndCodeAndStatus(Long userId, String code, UserVoucherStatus status);
}

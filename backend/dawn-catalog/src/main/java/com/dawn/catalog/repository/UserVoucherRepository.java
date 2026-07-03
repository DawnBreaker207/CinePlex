package com.dawn.catalog.repository;

import com.dawn.catalog.model.UserVoucher;
import com.dawn.common.core.constant.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserId(Long userId);

    List<UserVoucher> findByUserIdAndStatus(Long userId, UserVoucherStatus status);

    Optional<UserVoucher> findByUserIdAndCodeAndStatus(Long userId, String code, UserVoucherStatus status);

    long countByUserIdAndVoucherIdAndStatus(Long userId, Long voucherId, UserVoucherStatus status);

    List<UserVoucher> findByUserIdOrderByClaimedAtDesc(Long userId);

    Optional<UserVoucher> findByUserIdAndCode(Long userId, String code);

    long countByUserIdAndVoucherId(Long userId, Long voucherId);

    @Modifying
    @Query("UPDATE UserVoucher uv SET uv.status = :status WHERE uv.userId = :userId AND uv.code = :code")
    void updateStatusByUserIdAndCode(@Param("userId") Long userId, @Param("code") String code, @Param("status") UserVoucherStatus status);
}

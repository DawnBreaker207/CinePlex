package com.dawn.catalog.repository;

import com.dawn.catalog.model.Voucher;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    @Override
    Page<Voucher> findAll(Pageable pageable);

    Optional<Voucher> findByCode(String code);

    @Modifying
    @Query("""
            UPDATE Voucher v SET v.quantityUsed = v.quantityUsed + 1
            WHERE v.code = :code
            AND v.isActive = true
            AND v.quantityUsed < v.quantityTotal
            AND :now BETWEEN v.startAt AND v.endAt
            """)
    int useVoucher(@Param("code") String code, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE Voucher v SET v.quantityUsed = v.quantityUsed - 1
            WHERE v.code = :code
            AND v.isActive = true
            AND v.quantityUsed > 0
            """)
    void releaseVoucher(@Param("code") String code);
}

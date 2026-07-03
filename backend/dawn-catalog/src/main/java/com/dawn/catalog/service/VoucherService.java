package com.dawn.catalog.service;

import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.UserVoucherResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {

    ResponsePage<VoucherResponse> getAll(Pageable pageable);

    VoucherResponse findByCode(String code);

    VoucherResponse create(VoucherRequest req);

    VoucherResponse update(Long id, VoucherRequest req);

    void delete(Long id);

    void useVoucher(String code, Long userId, String reservationId);

    void releaseVoucher(String code, Long userId);

    VoucherCalculation calculate(String code, BigDecimal total);

    UserVoucherResponse claimVoucher(String code, Long userId);

    List<UserVoucherResponse> getUserVouchers(Long userId);
}

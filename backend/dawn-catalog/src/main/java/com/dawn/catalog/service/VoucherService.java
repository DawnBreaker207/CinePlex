package com.dawn.catalog.service;


import com.dawn.catalog.dto.request.VoucherRequest;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.catalog.dto.response.VoucherResponse;
import com.dawn.catalog.model.Voucher;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    ResponsePage<VoucherResponse> getAll(Pageable pageable);

    VoucherResponse findByCode(String code);

    VoucherResponse create(VoucherRequest req);

    VoucherResponse update(Long id, VoucherRequest req);

    void delete(Long id);

    void useVoucher(String code);

    void releaseVoucher(String code);

    VoucherCalculation calculate(String code, BigDecimal total);
}

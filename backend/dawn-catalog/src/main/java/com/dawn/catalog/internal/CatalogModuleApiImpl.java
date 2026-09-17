package com.dawn.catalog.internal;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogModuleApiImpl implements CatalogModuleApi {

    private final MovieService movieService;

    private final VoucherService voucherService;

    @Override
    public List<MovieResponse> findMoviesByIds(List<Long> ids) {
        return movieService.findAllByIds(ids);
    }

    @Override
    public MovieResponse findMovieById(Long id) {
        return movieService.findById(id);
    }

    @Override
    public VoucherCalculation calculateVoucher(String code, BigDecimal totalAmount) {
        return voucherService.calculate(code, totalAmount);
    }

    @Override
    public void useVoucher(String code, Long userId, String reservationId) {
        voucherService.useVoucher(code, userId, reservationId);
    }

    @Override
    public void releaseVoucher(String code, Long userId) {
        voucherService.releaseVoucher(code, userId);
    }
}

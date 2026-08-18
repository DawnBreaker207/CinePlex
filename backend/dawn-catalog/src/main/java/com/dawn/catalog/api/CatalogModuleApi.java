package com.dawn.catalog.api;

import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogModuleApi {

    List<MovieResponse> findMoviesByIds(List<Long> ids);

    MovieResponse findMovieById(Long id);

    VoucherCalculation calculateVoucher(String code, BigDecimal totalAmount);

    void useVoucher(String code, Long userId, String reservationId);

    void releaseVoucher(String code, Long userId);
}

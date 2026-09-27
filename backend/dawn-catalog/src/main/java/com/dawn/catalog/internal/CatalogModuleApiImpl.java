package com.dawn.catalog.internal;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.request.MovieRequest;
import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.ReviewResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.common.core.dto.response.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogModuleApiImpl implements CatalogModuleApi {

    private final MovieService movieService;

    private final VoucherService voucherService;

    private final ReviewService reviewService;

    @Override
    public List<MovieResponse> findMoviesByIds(List<Long> ids) {
        return movieService.findAllByIds(ids);
    }

    @Override
    public MovieResponse findMovieById(Long id) {
        return movieService.findById(id);
    }

    @Override
    public ResponsePage<MovieResponse> findAllMovies(Pageable pageable) {
        return movieService.findAll(new MovieRequest(), pageable);
    }

    @Override
    public List<ReviewResponse> findPendingReviews() {
        return reviewService.findPendingReviews();
    }

    @Override
    public Optional<ReviewResponse> findReviewById(Long reviewId) {
        return reviewService.findById(reviewId);
    }

    @Override
    public Optional<ReviewResponse> moderateReview(Long reviewId, boolean approve) {
        return reviewService.moderate(reviewId, approve);
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

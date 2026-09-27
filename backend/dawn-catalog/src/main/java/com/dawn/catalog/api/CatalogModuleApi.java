package com.dawn.catalog.api;

import com.dawn.catalog.dto.response.MovieResponse;
import com.dawn.catalog.dto.response.ReviewResponse;
import com.dawn.catalog.dto.response.VoucherCalculation;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CatalogModuleApi {

    List<MovieResponse> findMoviesByIds(List<Long> ids);

    MovieResponse findMovieById(Long id);

    ResponsePage<MovieResponse> findAllMovies(Pageable pageable);

    VoucherCalculation calculateVoucher(String code, BigDecimal totalAmount);

    void useVoucher(String code, Long userId, String reservationId);

    void releaseVoucher(String code, Long userId);

    List<ReviewResponse> findPendingReviews();

    Optional<ReviewResponse> findReviewById(Long reviewId);

    Optional<ReviewResponse> moderateReview(Long reviewId, boolean approve);
}

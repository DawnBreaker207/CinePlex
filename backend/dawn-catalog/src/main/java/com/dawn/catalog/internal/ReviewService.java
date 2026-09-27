package com.dawn.catalog.internal;

import com.dawn.catalog.dto.response.ReviewResponse;

import java.util.List;
import java.util.Optional;

public interface ReviewService {
    List<ReviewResponse> findPendingReviews();

    Optional<ReviewResponse> findById(Long reviewId);

    Optional<ReviewResponse> moderate(Long reviewId, boolean approve);
}

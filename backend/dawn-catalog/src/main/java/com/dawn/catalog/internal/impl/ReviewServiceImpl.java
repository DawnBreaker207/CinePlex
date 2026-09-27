package com.dawn.catalog.internal.impl;

import com.dawn.catalog.dto.response.ReviewResponse;
import com.dawn.catalog.internal.ReviewService;
import com.dawn.catalog.model.Review;
import com.dawn.catalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public List<ReviewResponse> findPendingReviews() {
        return reviewRepository.findTop20ByModeratedFalseOrderByIdAsc().stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Optional<ReviewResponse> findById(Long reviewId) {
        return reviewRepository.findById(reviewId).map(this::map);
    }

    @Override
    public Optional<ReviewResponse> moderate(Long reviewId, boolean approve) {
        return reviewRepository.findById(reviewId).map(review -> {
            review.setModerated(true);
            review.setIsActive(approve);
            return map(reviewRepository.save(review));
        });
    }

    private ReviewResponse map(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .movieId(review.getMovieId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isActive(review.getIsActive())
                .moderated(review.getModerated())
                .build();
    }
}

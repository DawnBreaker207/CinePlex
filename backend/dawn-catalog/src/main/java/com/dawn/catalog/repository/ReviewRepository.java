package com.dawn.catalog.repository;

import com.dawn.catalog.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMovieId(Long movieId);
    List<Review> findByUserId(Long userId);
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);
    Page<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId, Pageable pageable);
}

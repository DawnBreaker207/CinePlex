package com.dawn.ai.service;

import com.dawn.catalog.model.Review;
import com.dawn.catalog.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewModerationTools")
class ReviewModerationToolsTest {

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    ReviewModerationTools tools;

    private static Review review(Long id, int rating, String comment) {
        Review r = Review.builder().userId(1L).movieId(2L).build();
        r.setId(id);
        r.setRating((byte) rating);
        r.setComment(comment);
        return r;
    }

    @Nested
    @DisplayName("getPendingReviews")
    class Pending {

        @Test
        @DisplayName("empty → friendly message")
        void empty_message() {
            when(reviewRepository.findTop20ByModeratedFalseOrderByIdAsc()).thenReturn(List.of());

            assertThat(tools.getPendingReviews()).isEqualTo("No pending reviews.");
        }

        @Test
        @DisplayName("lists pending with key fields")
        void lists_fields() {
            when(reviewRepository.findTop20ByModeratedFalseOrderByIdAsc())
                    .thenReturn(List.of(review(7L, 5, "Great!")));

            assertThat(tools.getPendingReviews()).contains("#7").contains("Great!");
        }
    }

    @Nested
    @DisplayName("analyzeReview")
    class Analyze {

        @Test
        @DisplayName("high rating → POSITIVE, clean scores")
        void positive_clean() {
            when(reviewRepository.findById(7L))
                    .thenReturn(Optional.of(review(7L, 5, "Great movie")));

            assertThat(tools.analyzeReview(7L))
                    .contains("POSITIVE").contains("spam_score=0.0").contains("toxic_flag=false");
        }

        @Test
        @DisplayName("spam link → high spam score")
        void spamLink_flagged() {
            when(reviewRepository.findById(8L))
                    .thenReturn(Optional.of(review(8L, 1, "Click https://spam to win")));

            assertThat(tools.analyzeReview(8L)).contains("toxic_flag=false");
            assertThat(tools.analyzeReview(8L)).contains("spam_score=1.0");
        }

        @Test
        @DisplayName("missing → not found")
        void missing_notFound() {
            when(reviewRepository.findById(9L)).thenReturn(Optional.empty());

            assertThat(tools.analyzeReview(9L)).contains("not found");
        }
    }

    @Nested
    @DisplayName("approveReview")
    class Approve {

        @Test
        @DisplayName("approve marks moderated and visible")
        void approve_marks() {
            Review r = review(7L, 5, "Great");
            when(reviewRepository.findById(7L)).thenReturn(Optional.of(r));

            assertThat(tools.approveReview(7L, true)).contains("approved");
            assertThat(r.getModerated()).isTrue();
            assertThat(r.getIsActive()).isTrue();
            verify(reviewRepository).save(r);
        }

        @Test
        @DisplayName("reject hides")
        void reject_hides() {
            Review r = review(7L, 1, "Spam https://x");
            when(reviewRepository.findById(7L)).thenReturn(Optional.of(r));

            tools.approveReview(7L, false);

            assertThat(r.getModerated()).isTrue();
            assertThat(r.getIsActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("suggestReviewReply")
    class Suggest {

        @Test
        @DisplayName("reply matches sentiment")
        void sentimentReply() {
            when(reviewRepository.findById(7L))
                    .thenReturn(Optional.of(review(7L, 5, "Great")));
            when(reviewRepository.findById(8L))
                    .thenReturn(Optional.of(review(8L, 1, "Bad")));

            assertThat(tools.suggestReviewReply(7L)).contains("Cảm ơn");
            assertThat(tools.suggestReviewReply(8L)).contains("xin lỗi");
        }
    }
}

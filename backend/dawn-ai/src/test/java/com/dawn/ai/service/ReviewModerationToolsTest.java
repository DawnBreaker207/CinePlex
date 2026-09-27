package com.dawn.ai.service;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.ReviewResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewModerationTools")
class ReviewModerationToolsTest {

    @Mock
    CatalogModuleApi catalogApi;

    @InjectMocks
    ReviewModerationTools tools;

    private static ReviewResponse review(Long id, int rating, String comment) {
        return ReviewResponse.builder()
                .id(id).userId(1L).movieId(2L).rating((byte) rating).comment(comment)
                .isActive(true).moderated(false)
                .build();
    }

    @Nested
    @DisplayName("getPendingReviews")
    class Pending {

        @Test
        @DisplayName("empty → friendly message")
        void empty_message() {
            when(catalogApi.findPendingReviews()).thenReturn(List.of());

            assertThat(tools.getPendingReviews()).isEqualTo("No pending reviews.");
        }

        @Test
        @DisplayName("lists pending with key fields")
        void lists_fields() {
            when(catalogApi.findPendingReviews())
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
            when(catalogApi.findReviewById(7L))
                    .thenReturn(Optional.of(review(7L, 5, "Great movie")));

            assertThat(tools.analyzeReview(7L))
                    .contains("POSITIVE").contains("spam_score=0.0").contains("toxic_flag=false");
        }

        @Test
        @DisplayName("spam link → high spam score")
        void spamLink_flagged() {
            when(catalogApi.findReviewById(8L))
                    .thenReturn(Optional.of(review(8L, 1, "Click https://spam to win")));

            assertThat(tools.analyzeReview(8L)).contains("toxic_flag=false");
            assertThat(tools.analyzeReview(8L)).contains("spam_score=1.0");
        }

        @Test
        @DisplayName("missing → not found")
        void missing_notFound() {
            when(catalogApi.findReviewById(9L)).thenReturn(Optional.empty());

            assertThat(tools.analyzeReview(9L)).contains("not found");
        }
    }

    @Nested
    @DisplayName("approveReview")
    class Approve {

        @Test
        @DisplayName("approve marks moderated and visible")
        void approve_marks() {
            when(catalogApi.moderateReview(7L, true))
                    .thenReturn(Optional.of(review(7L, 5, "Great")));

            assertThat(tools.approveReview(7L, true)).contains("approved");
        }

        @Test
        @DisplayName("reject hides")
        void reject_hides() {
            when(catalogApi.moderateReview(7L, false))
                    .thenReturn(Optional.of(review(7L, 1, "Spam https://x")));

            assertThat(tools.approveReview(7L, false)).contains("rejected");
        }
    }

    @Nested
    @DisplayName("suggestReviewReply")
    class Suggest {

        @Test
        @DisplayName("reply matches sentiment")
        void sentimentReply() {
            when(catalogApi.findReviewById(7L))
                    .thenReturn(Optional.of(review(7L, 5, "Great")));
            when(catalogApi.findReviewById(8L))
                    .thenReturn(Optional.of(review(8L, 1, "Bad")));

            assertThat(tools.suggestReviewReply(7L)).contains("Cảm ơn");
            assertThat(tools.suggestReviewReply(8L)).contains("xin lỗi");
        }
    }
}

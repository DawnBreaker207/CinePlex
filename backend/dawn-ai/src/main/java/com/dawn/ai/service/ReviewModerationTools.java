package com.dawn.ai.service;

import com.dawn.catalog.api.CatalogModuleApi;
import com.dawn.catalog.dto.response.ReviewResponse;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewModerationTools {

    private static final Set<String> SPAM_HINTS = Set.of(
            "http://", "https://", "trung thuong", "trúng thưởng", "click", "kiem tien",
            "kiếm tiền", "free money", "giveaway", "khuyen mai khung");
    private static final Set<String> TOXIC_HINTS = Set.of(
            "fuck", "shit", "bitch", "dm", "dmm", "cc", "vl", "vcl", "ngu ", "nguoi");

    private final CatalogModuleApi catalogApi;

    @Tool("List unmoderated reviews (newest first by id, up to 20).")
    public String getPendingReviews() {
        List<ReviewResponse> pending = catalogApi.findPendingReviews();
        if (pending.isEmpty()) {
            return "No pending reviews.";
        }
        return pending.stream()
                .map(r -> String.format("#%d movie=%d user=%d rating=%d: %s",
                        r.getId(), r.getMovieId(), r.getUserId(), r.getRating(),
                        r.getComment() == null ? "" : r.getComment()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Analyze one review: sentiment from rating, spam score and toxic flag from keyword scan.")
    public String analyzeReview(Long reviewId) {
        return catalogApi.findReviewById(reviewId)
                .map(this::analyze)
                .orElse("Review not found: " + reviewId);
    }

    @Tool("Approve or reject a review; approved stays visible, rejected is hidden. Marks it moderated.")
    public String approveReview(Long reviewId, boolean approve) {
        return catalogApi.moderateReview(reviewId, approve)
                .map(review -> "Review #" + reviewId + (approve ? " approved." : " rejected and hidden."))
                .orElse("Review not found: " + reviewId);
    }

    @Tool("Suggest an admin reply draft for a review based on its sentiment.")
    public String suggestReviewReply(Long reviewId) {
        return catalogApi.findReviewById(reviewId).map(review -> {
            String sentiment = sentimentOf(review.getRating());
            return switch (sentiment) {
                case "POSITIVE" -> "Cảm ơn bạn đã đánh giá " + review.getRating()
                        + "/5! CinePlex rất vui khi bạn hài lòng. Hẹn gặp lại ở suất chiếu tiếp theo.";
                case "NEGATIVE" -> "CinePlex xin lỗi về trải nghiệm chưa tốt của bạn. "
                        + "Bạn vui lòng cho biết chi tiết để chúng tôi cải thiện. "
                        + "Tham khảo đơn hàng của bạn để hỗ trợ tốt nhất.";
                default -> "Cảm ơn bạn đã đánh giá! Ý kiến của bạn giúp CinePlex cải thiện dịch vụ mỗi ngày.";
            };
        }).orElse("Review not found: " + reviewId);
    }

    private String analyze(ReviewResponse review) {
        String text = review.getComment() == null ? "" : review.getComment().toLowerCase(Locale.ROOT);
        long spamHits = SPAM_HINTS.stream().filter(text::contains).count();
        boolean toxic = TOXIC_HINTS.stream().anyMatch(text::contains);
        double spamScore = Math.min(1.0, spamHits * 0.5);
        return String.format("Review #%d: sentiment=%s, spam_score=%.1f, toxic_flag=%s",
                review.getId(), sentimentOf(review.getRating()), spamScore, toxic);
    }

    private String sentimentOf(Byte rating) {
        if (rating != null && rating >= 4) {
            return "POSITIVE";
        }
        if (rating != null && rating == 3) {
            return "NEUTRAL";
        }
        return "NEGATIVE";
    }
}

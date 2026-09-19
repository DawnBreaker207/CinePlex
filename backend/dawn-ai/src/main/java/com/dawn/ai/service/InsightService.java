package com.dawn.ai.service;

import com.dawn.ai.dto.DailyForecast;
import com.dawn.ai.dto.InsightResponse;
import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.DashboardResponse;
import com.dawn.report.dto.response.PaymentDistribution;
import com.dawn.report.dto.response.TopMovieResponse;
import com.dawn.report.dto.response.TopTheaterResponse;
import com.dawn.report.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// Rule-based admin insights (no LLM): derive short advisory notes from the
// same reporting data the dashboard shows. Every rule degrades to nothing
// when the underlying source is empty/fails, so insights() never throws.
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private static final int FORECAST_DAYS = 7;
    private static final double LOW_SATURATION = 20.0;
    private static final double HIGH_SATURATION = 90.0;
    private static final double PAYMENT_DOMINANCE = 0.5;

    private final DashboardService dashboardService;
    private final ForecastService forecastService;

    public List<InsightResponse> insights() {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(start)
                    .endDate(end)
                    .build();

            CompletableFuture<DashboardResponse> summary = CompletableFuture.supplyAsync(() -> {
                try {
                    return dashboardService.getSummary(filter);
                } catch (RuntimeException e) {
                    log.warn("Insights summary unavailable", e);
                    return null;
                }
            });
            CompletableFuture<List<DailyForecast>> forecast = CompletableFuture.supplyAsync(() -> {
                try {
                    return forecastService.forecastRevenue(FORECAST_DAYS);
                } catch (RuntimeException e) {
                    log.warn("Insights forecast unavailable", e);
                    return List.of();
                }
            });

            List<InsightResponse> out = new ArrayList<>();
            DashboardResponse s = summary.join();
            if (s != null) {
                out.addAll(topMovieInsights(s));
                out.addAll(paymentInsights(s));
                out.addAll(saturationInsights(s));
            }
            out.addAll(forecastInsights(forecast.join()));
            return out;
        } catch (RuntimeException e) {
            log.warn("Insights generation failed", e);
            return List.of();
        }
    }

    // Best/worst days of the upcoming week vs. today — tells the admin where
    // to staff up. Derived from the same day-of-week averages as the chart.
    private List<InsightResponse> forecastInsights(List<DailyForecast> forecast) {
        if (forecast == null || forecast.isEmpty()) {
            return List.of();
        }
        List<InsightResponse> out = new ArrayList<>();
        Optional<DailyForecast> best = forecast.stream()
                .max(Comparator.comparingDouble(DailyForecast::projectedRevenue));
        Optional<DailyForecast> worst = forecast.stream()
                .filter(f -> f.projectedRevenue() > 0)
                .min(Comparator.comparingDouble(DailyForecast::projectedRevenue));
        best.ifPresent(f -> out.add(trend(
                "FORECAST_PEAK",
                "success",
                weekday(f.date()),
                String.format("Dự báo doanh thu %s đạt %s VNĐ — cao nhất 7 ngày tới.",
                        weekday(f.date()), formatVnd(f.projectedRevenue())))));
        worst.filter(f -> f.projectedRevenue() < best.map(DailyForecast::projectedRevenue).orElse(0.0))
                .ifPresent(f -> out.add(trend(
                        "FORECAST_TROUGH",
                        "warning",
                        weekday(f.date()),
                        String.format("Dự báo %s chỉ đạt %s VNĐ, thấp nhất tuần — cân nhắc khuyến mãi khung giờ vắng.",
                                weekday(f.date()), formatVnd(f.projectedRevenue())))));
        return out;
    }

    private List<InsightResponse> topMovieInsights(DashboardResponse s) {
        List<TopMovieResponse> movies = s.getMovies();
        if (movies == null || movies.isEmpty()) {
            return List.of();
        }
        TopMovieResponse top = movies.get(0);
        return List.of(trend(
                "TOP_MOVIE",
                "info",
                top.getMovieName(),
                String.format("Phim dẫn đầu 30 ngày: %s — %d vé, %s VNĐ.",
                        top.getMovieName(), top.getTicketSold(), formatVnd(top.getRevenue()))));
    }

    private List<InsightResponse> paymentInsights(DashboardResponse s) {
        List<PaymentDistribution> payments = s.getPayments();
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        double total = payments.stream().mapToLong(PaymentDistribution::getAmount).sum();
        if (total <= 0) {
            return List.of();
        }
        PaymentDistribution top = payments.stream()
                .max(Comparator.comparingLong(PaymentDistribution::getAmount))
                .orElse(null);
        if (top == null) {
            return List.of();
        }
        double share = top.getAmount() / total;
        if (share >= PAYMENT_DOMINANCE) {
            return List.of(trend(
                    "PAYMENT_DOMINANT",
                    "warning",
                    top.getMethod(),
                    String.format("%s chiếm %d%% tổng doanh thu — rủi ro tập trung kênh, nên theo dõi phí giao dịch.",
                            top.getMethod(), Math.round(share * 100))));
        }
        return List.of();
    }

    private List<InsightResponse> saturationInsights(DashboardResponse s) {
        Double saturation = s.getMetrics() == null ? null : s.getMetrics().getSeatUtilization();
        if (saturation == null) {
            return List.of();
        }
        if (saturation >= HIGH_SATURATION) {
            return List.of(trend(
                    "SATURATION_HIGH",
                    "success",
                    "Công suất ghế",
                    String.format("Công suất ghế trung bình đang ở mức %d%% — giờ cao điểm sắp kín chỗ, hãy mở thêm suất chiếu.",
                            Math.round(saturation))));
        }
        if (saturation <= LOW_SATURATION) {
            return List.of(trend(
                    "SATURATION_LOW",
                    "warning",
                    "Công suất ghế",
                    String.format("Công suất ghế chỉ %d%% — cân nhắc gói khuyến mãi để lấp khung giờ vắng.",
                            Math.round(saturation))));
        }
        return List.of();
    }

    private InsightResponse trend(String type, String severity, String title, String detail) {
        return new InsightResponse(type, severity, title, detail);
    }

    private String weekday(LocalDate date) {
        String name = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi"));
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String formatVnd(double amount) {
        return new DecimalFormat("#,###").format(Math.round(amount));
    }
}
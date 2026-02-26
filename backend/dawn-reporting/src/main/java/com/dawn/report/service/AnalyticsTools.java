package com.dawn.report.service;

import com.dawn.common.core.dto.request.DatetimeFilterRequest;
import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.MetricsResponse;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsTools {
    private final DashboardService dashboardService;

    @Tool("Lấy các chỉ số tổng quan (Doanh thu, vé bán, rạp hoạt động) trong khoảng thời gian. Date format: YYYY-MM-DD")
    public String getMetrics(String startDate, String endDate, Long movieId, Long theaterId) {
        try {
            DashboardFilterRequest req = createRequest(startDate, endDate, movieId, theaterId);
            MetricsResponse metrics = dashboardService.getMetrics(req);


            return String.format("""
                            Dữ liệu từ %s đến %s:
                            - Tổng doanh thu: %s
                            - Số vé bán ra: %d
                            - Số rạp hoạt động: %d
                            - Tỉ lệ lấp đầy ghế: %.2f%%
                            """,
                    req.getStartDate(), req.getEndDate(),
                    formatMoney(metrics.getTotalRevenue()),
                    metrics.getTicketsSold(),
                    metrics.getActiveTheaters(),
                    metrics.getSeatUtilization());
        } catch (Exception e) {
            return "Lỗi khi lấy dữ liệu: " + e.getMessage();
        }
    }

    @Tool("Lấy dữ liệu biểu đồ xu hướng doanh thu theo thời gian")
    public String getRevenueTrend(String startDate, String endDate, Long theaterId) {
        DashboardFilterRequest req = createRequest(startDate, endDate, null, theaterId);
        var data = dashboardService.getRevenueOverTime(req);

        if (data.isEmpty()) return "Không có dữ liệu doanh thu trong giai đoạn này";

        return data
                .stream()
                .map(d -> String.format("[%s: %s]", d.getDate(), formatMoney(d.getRevenue())))
                .collect(Collectors.joining(", "));
    }

    @Tool("Lấy danh sách các phim có doanh thu cao nhất")
    public String getTopMovies(String startDate, String endDate) {
        DashboardFilterRequest req = createRequest(startDate, endDate, null, null);
        var data = dashboardService.getTopMovies(req);

        return data
                .stream()
                .map(m -> String.format("- %s (Doanh thu: %s)", m.getMovieName(), formatMoney(m.getRevenue())))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Lấy danh sách các rạp phim có doanh thu cao nhất")
    public String getTopTheaters(String startDate, String endDate) {
        DashboardFilterRequest req = createRequest(startDate, endDate, null, null);
        var data = dashboardService.getTopTheaters(req);

        return data
                .stream()
                .map(m -> String.format("- %s (Doanh thu: %s)", m.getTheaterName(), formatMoney(m.getTotalRevenue())))
                .collect(Collectors.joining("\n"));
    }


    private String formatMoney(Number amount) {
        if (amount == null) return "0 VNĐ";
        return String.format("%,.0f VNĐ", amount.doubleValue());
    }

    private DashboardFilterRequest createRequest(String startDate, String endDate, Long movieId, Long theaterId) {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        try {
            if (startDate != null && !startDate.isBlank() && !"null".equalsIgnoreCase(startDate)) {
                start = LocalDate.parse(startDate);
            }
            if (endDate != null && !endDate.isBlank() && !"null".equalsIgnoreCase(endDate)) {
                end = LocalDate.parse(endDate);
            }
        } catch (DateTimeParseException e) {
            log.warn("AI sent invalid date format. Start: {}, End: {}. Using default.", startDate, endDate);
        }

        Long finalMovieId = (movieId != null && movieId == 0) ? null : movieId;
        Long finalTheaterId = (theaterId != null && theaterId == 0) ? null : theaterId;

        return DashboardFilterRequest.builder()
                .startDate(start)
                .endDate(end)
                .movieId(finalMovieId)
                .theaterId(finalTheaterId)
                .build();
    }
}

package com.dawn.report.service.impl;

import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.*;
import com.dawn.report.repository.DashboardRepository;
import com.dawn.report.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboardRepository;

    @Override
    public DashboardResponse getSummary(DashboardFilterRequest req) {

        CompletableFuture<MetricsResponse> metrics = CompletableFuture.supplyAsync(() -> getMetrics(req));
        CompletableFuture<List<RevenuePointResponse>> revenue = CompletableFuture.supplyAsync(() -> getRevenueOverTime(req));
        CompletableFuture<List<TopMovieResponse>> topMovie = CompletableFuture.supplyAsync(() -> getTopMovies(req));
        CompletableFuture<List<TopTheaterResponse>> topTheater = CompletableFuture.supplyAsync(() -> getTopTheaters(req));
        CompletableFuture<List<PaymentDistribution>> paymentDistribution = CompletableFuture.supplyAsync(() -> getPaymentDistribution(req));

        CompletableFuture.allOf(metrics, revenue, topMovie, topMovie, paymentDistribution).join();
        try {
            return DashboardResponse
                    .builder()
                    .metrics(metrics.get())
                    .revenues(revenue.get())
                    .movies(topMovie.get())
                    .theaters(topTheater.get())
                    .payments(paymentDistribution.get())
                    .build();
        } catch (Exception e) {
            log.error("Error in dashboard: {}", e.getMessage());
            throw new RuntimeException("Can not get data dashboard");
        }

    }

    @Override
    @Cacheable(value = "dashboard:metrics", key = "#req.movieId + ':' + #req.theaterId")
    public MetricsResponse getMetrics(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

        Double totalRevenue = dashboardRepository.getTotalRevenue(start, end, req.getMovieId(), req.getTheaterId());
        Long ticketSold = dashboardRepository.getTicketsSold(start, end, req.getMovieId(), req.getTheaterId());
        Long activeTheaters = dashboardRepository.getActiveTheaters(start, end, req.getMovieId(), req.getTheaterId());
        Double seatUtilization = dashboardRepository.getSeatUtilization(start, end, req.getTheaterId());

        return MetricsResponse
                .builder()
                .totalRevenue(totalRevenue)
                .ticketsSold(ticketSold)
                .activeTheaters(activeTheaters)
                .seatUtilization(seatUtilization)
                .build();
    }

    @Override
    @Cacheable(value = "dashboard:revenue", key = "#req.movieId + ':' + #req.theaterId")
    public List<RevenuePointResponse> getRevenueOverTime(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

        return dashboardRepository.getRevenueOverTime(start, end, req.getTheaterId());
    }

    @Override
    @Cacheable(value = "dashboard:topMovies", key = "#req.movieId + ':' + #req.theaterId")
    public List<TopMovieResponse> getTopMovies(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

        return dashboardRepository.getTopMovies(start, end);

    }

    @Override
    @Cacheable(value = "dashboard:topTheaters", key = "#req.movieId + ':' + #req.theaterId")
    public List<TopTheaterResponse> getTopTheaters(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

        return dashboardRepository.getTopTheaters(start, end);

    }

    @Override
    @Cacheable(value = "dashboard:paymentDistribution", key = "#req.movieId + ':' + #req.theaterId")
    public List<PaymentDistribution> getPaymentDistribution(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(30);

        return dashboardRepository.getPaymentDistribution(start, end);

    }

}

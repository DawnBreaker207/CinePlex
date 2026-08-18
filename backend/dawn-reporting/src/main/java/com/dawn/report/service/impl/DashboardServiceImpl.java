package com.dawn.report.service.impl;

import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.*;
import com.dawn.report.repository.DashboardRepository;
import com.dawn.report.service.DashboardService;
import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.Message;
import com.dawn.common.core.exception.wrapper.InternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboardRepository;

    @Override
    public DashboardResponse getSummary(DashboardFilterRequest req) {
        try {
            CompletableFuture<MetricsResponse> metrics = CompletableFuture.supplyAsync(() -> getMetrics(req));
            CompletableFuture<List<RevenuePointResponse>> revenue = CompletableFuture.supplyAsync(() -> getRevenueOverTime(req));
            CompletableFuture<List<TopMovieResponse>> topMovie = CompletableFuture.supplyAsync(() -> getTopMovies(req));
            CompletableFuture<List<TopTheaterResponse>> topTheater = CompletableFuture.supplyAsync(() -> getTopTheaters(req));
            CompletableFuture<List<PaymentDistribution>> paymentDistribution = CompletableFuture.supplyAsync(() -> getPaymentDistribution(req));

            CompletableFuture.allOf(metrics, revenue, topMovie, topTheater, paymentDistribution).join();

            return DashboardResponse
                    .builder()
                    .metrics(metrics.get())
                    .revenues(revenue.get())
                    .movies(topMovie.get())
                    .theaters(topTheater.get())
                    .payments(paymentDistribution.get())
                    .build();
        } catch (ExecutionException e) {
            log.error("Error in dashboard: {}", e.getMessage(), e);
            throw new InternalServiceException(Message.Exception.DASHBOARD_CAN_NOT_GET_DATA, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error in dashboard: {}", e.getMessage(), e);
            throw new InternalServiceException(Message.Exception.DASHBOARD_CAN_NOT_GET_DATA, e);
        }
    }

    @Override
    @Cacheable(value = Constants.CACHE_DASHBOARD_METRICS, key = "#req.movieId + ':' + #req.theaterId")
    public MetricsResponse getMetrics(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

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
    @Cacheable(value = Constants.CACHE_DASHBOARD_REVENUE, key = "#req.movieId + ':' + #req.theaterId")
    public List<RevenuePointResponse> getRevenueOverTime(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

        return dashboardRepository.getRevenueOverTime(start, end, req.getTheaterId());
    }

    @Override
    @Cacheable(value = Constants.CACHE_DASHBOARD_TOP_MOVIES, key = "#req.movieId + ':' + #req.theaterId")
    public List<TopMovieResponse> getTopMovies(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

        return dashboardRepository.getTopMovies(start, end);

    }

    @Override
    @Cacheable(value = Constants.CACHE_DASHBOARD_TOP_THEATERS, key = "#req.movieId + ':' + #req.theaterId")
    public List<TopTheaterResponse> getTopTheaters(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

        return dashboardRepository.getTopTheaters(start, end);

    }

    @Override
    @Cacheable(value = Constants.CACHE_DASHBOARD_PAYMENT_DISTRIBUTION, key = "#req.movieId + ':' + #req.theaterId")
    public List<PaymentDistribution> getPaymentDistribution(DashboardFilterRequest req) {
        LocalDate end = req.getEndDate() != null ? req.getEndDate() : LocalDate.now();
        LocalDate start = req.getStartDate() != null ? req.getStartDate() : end.minusDays(Constants.DEFAULT_DASHBOARD_DAYS);

        return dashboardRepository.getPaymentDistribution(start, end);

    }

}

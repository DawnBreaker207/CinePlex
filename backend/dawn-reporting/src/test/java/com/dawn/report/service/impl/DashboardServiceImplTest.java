package com.dawn.report.service.impl;

import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.*;
import com.dawn.report.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl")
class DashboardServiceImplTest {

    @Mock
    DashboardRepository dashboardRepository;

    DashboardServiceImpl service;

    DashboardFilterRequest req;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(dashboardRepository);
        req = DashboardFilterRequest.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("returns all fields correctly with async composition")
        void getSummary_shouldReturnAllFields() {
            when(dashboardRepository.getTopMovies(any(), any())).thenReturn(List.of(
                    TopMovieResponse.builder().movieName("Movie A").ticketSold(100L).revenue(50000L).build()
            ));
            when(dashboardRepository.getTopTheaters(any(), any())).thenReturn(List.of(
                    TopTheaterResponse.builder().theaterName("Theater 1").ticketsSold(200L).totalRevenue(30000L).build()
            ));
            when(dashboardRepository.getTotalRevenue(any(), any(), any(), any())).thenReturn(100000.0);
            when(dashboardRepository.getTicketsSold(any(), any(), any(), any())).thenReturn(500L);
            when(dashboardRepository.getActiveTheaters(any(), any(), any(), any())).thenReturn(3L);
            when(dashboardRepository.getSeatUtilization(any(), any(), any())).thenReturn(75.5);
            when(dashboardRepository.getRevenueOverTime(any(), any(), any())).thenReturn(List.of(
                    RevenuePointResponse.builder().date(LocalDate.of(2026, 1, 1)).revenue(10000L).build()
            ));
            when(dashboardRepository.getPaymentDistribution(any(), any())).thenReturn(List.of(
                    PaymentDistribution.builder().method("VNPAY").amount(60L).build()
            ));

            DashboardResponse result = service.getSummary(req);

            assertThat(result).isNotNull();
            assertThat(result.getMovies()).hasSize(1);
            assertThat(result.getMovies().get(0).getMovieName()).isEqualTo("Movie A");
            assertThat(result.getTheaters()).hasSize(1);
            assertThat(result.getTheaters().get(0).getTheaterName()).isEqualTo("Theater 1");
            assertThat(result.getMetrics()).isNotNull();
            assertThat(result.getMetrics().getTotalRevenue()).isEqualTo(100000.0);
            assertThat(result.getRevenues()).hasSize(1);
            assertThat(result.getPayments()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getMetrics")
    class GetMetrics {

        @Test
        @DisplayName("returns correct metrics from repository")
        void getMetrics_shouldReturnMetrics() {
            when(dashboardRepository.getTotalRevenue(any(), any(), any(), any())).thenReturn(100000.0);
            when(dashboardRepository.getTicketsSold(any(), any(), any(), any())).thenReturn(500L);
            when(dashboardRepository.getActiveTheaters(any(), any(), any(), any())).thenReturn(3L);
            when(dashboardRepository.getSeatUtilization(any(), any(), any())).thenReturn(75.5);

            MetricsResponse metrics = service.getMetrics(req);

            assertThat(metrics.getTotalRevenue()).isEqualTo(100000.0);
            assertThat(metrics.getTicketsSold()).isEqualTo(500L);
            assertThat(metrics.getActiveTheaters()).isEqualTo(3L);
            assertThat(metrics.getSeatUtilization()).isEqualTo(75.5);
        }
    }

    @Nested
    @DisplayName("getTopMovies")
    class GetTopMovies {

        @Test
        @DisplayName("returns top movies from repository")
        void getTopMovies_shouldReturnList() {
            when(dashboardRepository.getTopMovies(any(), any())).thenReturn(List.of(
                    TopMovieResponse.builder().movieName("Movie A").ticketSold(100L).revenue(50000L).build()
            ));

            List<TopMovieResponse> movies = service.getTopMovies(req);

            assertThat(movies).hasSize(1);
            assertThat(movies.get(0).getMovieName()).isEqualTo("Movie A");
        }
    }

    @Nested
    @DisplayName("getTopTheaters")
    class GetTopTheaters {

        @Test
        @DisplayName("returns top theaters from repository")
        void getTopTheaters_shouldReturnList() {
            when(dashboardRepository.getTopTheaters(any(), any())).thenReturn(List.of(
                    TopTheaterResponse.builder().theaterName("Theater 1").ticketsSold(200L).totalRevenue(30000L).build()
            ));

            List<TopTheaterResponse> theaters = service.getTopTheaters(req);

            assertThat(theaters).hasSize(1);
            assertThat(theaters.get(0).getTheaterName()).isEqualTo("Theater 1");
        }
    }
}

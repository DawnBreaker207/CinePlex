package com.dawn.ai.service;

import com.dawn.report.dto.response.RevenuePointResponse;
import com.dawn.report.service.DashboardService;
import com.dawn.ai.dto.DailyForecast;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastService")
class ForecastServiceTest {

    @Mock
    DashboardService dashboardService;

    @InjectMocks
    ForecastService service;

    private static RevenuePointResponse point(LocalDate date, long revenue) {
        return RevenuePointResponse.builder().date(date).revenue(revenue).build();
    }

    private static LocalDate pastMonday(int weeksAgo) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays((today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7);
        return monday.minusWeeks(weeksAgo);
    }

    @Nested
    @DisplayName("forecastRevenue")
    class ForecastRevenue {

        @Test
        @DisplayName("averages same weekday history")
        void averagesSameWeekday() {
            when(dashboardService.getRevenueOverTime(any())).thenReturn(List.of(
                    point(pastMonday(1), 100L),
                    point(pastMonday(2), 300L)));

            List<DailyForecast> result = service.forecastRevenue(7);

            assertThat(result).hasSize(7);
            DailyForecast nextMonday = result.stream()
                    .filter(f -> f.dayOfWeek().equals(DayOfWeek.MONDAY.name()))
                    .findFirst()
                    .orElseThrow();
            assertThat(nextMonday.projectedRevenue()).isEqualTo(200.0);
            assertThat(nextMonday.sampleDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("history down → empty forecast")
        void historyDown_empty() {
            when(dashboardService.getRevenueOverTime(any())).thenThrow(new RuntimeException("db down"));

            assertThat(service.forecastRevenue(7)).isEmpty();
        }

        @Test
        @DisplayName("days capped at 30")
        void daysCapped() {
            when(dashboardService.getRevenueOverTime(any())).thenReturn(List.of());

            assertThat(service.forecastRevenue(500)).hasSize(30);
            assertThat(service.forecastRevenue(0)).hasSize(7);
        }
    }
}

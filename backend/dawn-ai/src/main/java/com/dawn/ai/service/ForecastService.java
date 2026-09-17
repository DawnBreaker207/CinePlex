package com.dawn.ai.service;

import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.service.DashboardService;
import com.dawn.ai.dto.DailyForecast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Read-only demand forecast (no auto-pricing); day-of-week average baseline.
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private static final int HISTORY_DAYS = 90;
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 30;

    private final DashboardService dashboardService;

    public List<DailyForecast> forecastRevenue(Integer days) {
        int horizon = (days == null || days < 1) ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        Map<DayOfWeek, double[]> totals = new EnumMap<>(DayOfWeek.class);
        try {
            dashboardService.getRevenueOverTime(DashboardFilterRequest.builder()
                            .startDate(LocalDate.now().minusDays(HISTORY_DAYS))
                            .endDate(LocalDate.now())
                            .build())
                    .forEach(p -> {
                        if (p.getDate() == null || p.getRevenue() == null) {
                            return;
                        }
                        double[] acc = totals.computeIfAbsent(p.getDate().getDayOfWeek(), d -> new double[2]);
                        acc[0] += p.getRevenue();
                        acc[1] += 1;
                    });
        } catch (RuntimeException e) {
            log.warn("Revenue history unavailable, forecast aborted", e);
            return List.of();
        }
        List<DailyForecast> out = new ArrayList<>();
        for (int i = 1; i <= horizon; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            double[] acc = totals.getOrDefault(date.getDayOfWeek(), new double[2]);
            double projected = acc[1] == 0 ? 0.0 : acc[0] / acc[1];
            out.add(new DailyForecast(date, date.getDayOfWeek().name(), projected, (int) acc[1]));
        }
        return out;
    }
}

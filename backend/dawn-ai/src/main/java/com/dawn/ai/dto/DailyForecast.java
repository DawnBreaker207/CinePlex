package com.dawn.ai.dto;

import java.time.LocalDate;

public record DailyForecast(
        LocalDate date,
        String dayOfWeek,
        double projectedRevenue,
        int sampleDays) {
}

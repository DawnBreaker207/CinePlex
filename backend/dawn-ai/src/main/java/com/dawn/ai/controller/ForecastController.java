package com.dawn.ai.controller;

import com.dawn.ai.dto.DailyForecast;
import com.dawn.ai.dto.InsightResponse;
import com.dawn.ai.service.ForecastService;
import com.dawn.ai.service.InsightService;
import com.dawn.common.core.dto.response.ResponseObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;
    private final InsightService insightService;

    @GetMapping("/forecast/revenue")
    public ResponseObject<List<DailyForecast>> forecastRevenue(
            @RequestParam(required = false) Integer days) {
        return ResponseObject.success(forecastService.forecastRevenue(days));
    }

    @GetMapping("/insights")
    public ResponseObject<List<InsightResponse>> insights() {
        return ResponseObject.success(insightService.insights());
    }
}

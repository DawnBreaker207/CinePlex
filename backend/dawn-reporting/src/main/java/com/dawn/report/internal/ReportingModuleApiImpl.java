package com.dawn.report.internal;

import com.dawn.report.api.ReportingModuleApi;
import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.DashboardResponse;
import com.dawn.report.dto.response.RevenuePointResponse;
import com.dawn.report.dto.response.TopMovieResponse;
import com.dawn.report.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingModuleApiImpl implements ReportingModuleApi {

    private final DashboardService dashboardService;

    @Override
    public List<TopMovieResponse> getTopMovies(DashboardFilterRequest req) {
        return dashboardService.getTopMovies(req);
    }

    @Override
    public DashboardResponse getSummary(DashboardFilterRequest req) {
        return dashboardService.getSummary(req);
    }

    @Override
    public List<RevenuePointResponse> getRevenueOverTime(DashboardFilterRequest req) {
        return dashboardService.getRevenueOverTime(req);
    }
}

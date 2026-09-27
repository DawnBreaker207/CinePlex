package com.dawn.report.api;

import com.dawn.report.dto.request.DashboardFilterRequest;
import com.dawn.report.dto.response.DashboardResponse;
import com.dawn.report.dto.response.RevenuePointResponse;
import com.dawn.report.dto.response.TopMovieResponse;

import java.util.List;

public interface ReportingModuleApi {
    List<TopMovieResponse> getTopMovies(DashboardFilterRequest req);

    DashboardResponse getSummary(DashboardFilterRequest req);

    List<RevenuePointResponse> getRevenueOverTime(DashboardFilterRequest req);
}

package com.dawn.report.service;


import com.dawn.report.dto.response.ReportResponse;

import java.time.LocalDate;

public interface ReportService {
    ReportResponse exportReport(String reportFormat, LocalDate fromDate, LocalDate toDate);
}

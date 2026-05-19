package com.dawn.report.service.impl;

import com.dawn.report.dto.response.PaymentDistribution;
import com.dawn.report.dto.response.ReportResponse;
import com.dawn.report.dto.response.RevenuePointResponse;
import com.dawn.report.dto.response.TopMovieResponse;
import com.dawn.report.repository.DashboardRepository;
import com.dawn.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final DashboardRepository dashboardRepository;

    @Override
    public ReportResponse exportReport(String reportFormat) {
        try {
            LocalDate fromDate = null;
            LocalDate toDate = null;

            // Take data from database
            Double totalRevenue = dashboardRepository.getTotalRevenue(fromDate, toDate, null, null);
            Long ticketsSold = dashboardRepository.getTicketsSold(fromDate, toDate, null, null);
            Long activeTheaters = dashboardRepository.getActiveTheaters(fromDate, toDate, null, null);
            Double seatUtilization = dashboardRepository.getSeatUtilization(fromDate, toDate, null);

            //
            List<TopMovieResponse> topMovies = dashboardRepository.getTopMovies(fromDate, toDate);
            List<PaymentDistribution> paymentDist = dashboardRepository.getPaymentDistribution(fromDate, toDate);
            List<RevenuePointResponse> revenueTrend = dashboardRepository.getRevenueOverTime(fromDate, toDate, null);


            // Prepare parameters
            Map<String, Object> parameters = new HashMap<>();

            parameters.put(JRParameter.REPORT_LOCALE, new Locale("vi","VN"));

            parameters.put("p_TotalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            parameters.put("p_TicketsSold", ticketsSold != null ? ticketsSold : 0L);
            parameters.put("p_ActiveTheaters", activeTheaters != null ? activeTheaters : 0L);
            parameters.put("p_SeatUtilization", seatUtilization != null ? seatUtilization : 0.0);


            parameters.put("ds_TopMovies", new JRBeanCollectionDataSource(topMovies));
            parameters.put("ds_PaymentDist", new JRBeanCollectionDataSource(paymentDist));

            List<Map<String, Object>> dataMap = new ArrayList<>();
            if(revenueTrend != null){
                for(RevenuePointResponse r: revenueTrend){
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", java.sql.Date.valueOf(r.getDate().toString()));
                    item.put("revenue", BigDecimal.valueOf(r.getRevenue()));
                    dataMap.add(item);
                }
            }
            parameters.put("ds_RevenueTrend", new JRBeanCollectionDataSource(dataMap));

            // Load file
            InputStream reportStream = new ClassPathResource("/report.jrxml").getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            log.info("report stream: {}", reportStream);

            // Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // Export by format
            byte[] reportBytes;
            String filename;
            String contentType;

            if ("html".equalsIgnoreCase(reportFormat)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                HtmlExporter exporter = new HtmlExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));
                exporter.exportReport();
                reportBytes = out.toByteArray();
                filename = "report.html";
                contentType = "text/html";
            } else if ("pdf".equalsIgnoreCase(reportFormat)) {
                reportBytes = JasperExportManager.exportReportToPdf(jasperPrint);
                filename = "report.pdf";
                contentType = "application/pdf";
            } else if ("excel".equalsIgnoreCase(reportFormat)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));

                SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
                config.setOnePagePerSheet(false);
                config.setDetectCellType(true);
                config.setCollapseRowSpan(false);
                config.setIgnoreGraphics(false);
                exporter.setConfiguration(config);

                exporter.exportReport();
                exporter.reset();
                reportBytes = out.toByteArray();
                filename = "report.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                out.close();
            } else {
                throw new IllegalArgumentException("Unsupported report format: " + reportFormat);
            }

            return new ReportResponse(reportBytes, filename, contentType);
        } catch (JRException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

package com.dawn.notification.service;

import com.dawn.report.dto.response.ReportResponse;
import com.dawn.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportMailJob {

    private final ReportService reportService;
    private final EmailService emailService;

    @Value("${app.report-mail.recipients:}")
    private String recipients;

    @Value("${app.report-mail.lookback-days:1}")
    private int lookbackDays;

    @Scheduled(cron = "${app.report-mail.cron:0 0 7 * * *}")
    public void sendDailyReport() {
        if (recipients == null || recipients.isBlank()) {
            return;
        }
        LocalDate day = LocalDate.now().minusDays(Math.max(lookbackDays, 1));
        ReportResponse report;
        try {
            report = reportService.exportReport("pdf", day, day);
        } catch (RuntimeException e) {
            log.error("Daily report export failed for {}", day, e);
            return;
        }
        Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(to -> !to.isEmpty())
                .forEach(to -> {
                    try {
                        emailService.sendReportEmail(to, report.getData(), report.getFilename());
                    } catch (RuntimeException e) {
                        log.error("Daily report mail failed for {}", to, e);
                    }
                });
    }
}

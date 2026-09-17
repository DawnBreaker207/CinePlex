package com.dawn.notification.service;

import com.dawn.report.dto.response.ReportResponse;
import com.dawn.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportMailJob")
class ReportMailJobTest {

    @Mock
    ReportService reportService;
    @Mock
    EmailService emailService;

    @InjectMocks
    ReportMailJob job;

    @Nested
    @DisplayName("sendDailyReport")
    class SendDailyReport {

        @Test
        @DisplayName("no recipients → silent skip")
        void noRecipients_skip() {
            ReflectionTestUtils.setField(job, "recipients", "  ");

            job.sendDailyReport();

            verify(reportService, never()).exportReport(any(), any(), any());
            verify(emailService, never()).sendReportEmail(any(), any(), any());
        }

        @Test
        @DisplayName("recipients set → export yesterday PDF and mail each")
        void configured_exportsAndMails() {
            ReflectionTestUtils.setField(job, "recipients", "a@x.com, b@x.com");
            ReportResponse report = ReportResponse.builder()
                    .data(new byte[]{1, 2}).filename("report.pdf").contentType("application/pdf")
                    .build();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            when(reportService.exportReport(eq("pdf"), eq(yesterday), eq(yesterday))).thenReturn(report);

            job.sendDailyReport();

            verify(emailService).sendReportEmail(eq("a@x.com"), eq(new byte[]{1, 2}), eq("report.pdf"));
            verify(emailService).sendReportEmail(eq("b@x.com"), eq(new byte[]{1, 2}), eq("report.pdf"));
        }

        @Test
        @DisplayName("export fails → no mail, no throw")
        void exportFails_noMail() {
            ReflectionTestUtils.setField(job, "recipients", "a@x.com");
            when(reportService.exportReport(any(), any(), any())).thenThrow(new RuntimeException("jasper down"));

            job.sendDailyReport();

            verify(emailService, never()).sendReportEmail(any(), any(), any());
        }
    }
}

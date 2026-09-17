package com.dawn.notification.service;

import com.dawn.common.core.dto.event.BookingCompleteEvent;
import com.dawn.common.core.utils.BarcodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    public void sendReservationEmail(BookingCompleteEvent event) {
        String barcodeBase64 = BarcodeUtils.generateCode128(event.reservationCode(), 300, 100);

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom("demo@gmail.com");
            messageHelper.setTo(event.to());
            messageHelper.setSubject("[Thông tin vé phim] - Đặt vé trực tuyến thành công / Your online ticket purchase has been successful");

            Context context = new Context();
            context.setVariable("name", event.name());
            context.setVariable("reservationId", event.reservationCode());
            context.setVariable("movieName", event.movieName());
            context.setVariable("theaterName", event.theaterName());
            context.setVariable("showtimeSession", event.showtimeSession());
            context.setVariable("seats", event.seats());
            context.setVariable("paymentTime", event.paymentTime());
            context.setVariable("total", event.total());
            context.setVariable("barcode", barcodeBase64);

            String html = templateEngine.process("email", context);
            messageHelper.setText(html, true);
        };

        try {
            mailSender.send(messagePreparator);
        } catch (MailException ex) {
            log.error("Exception occurred when sending email to {} with message: {}", event.to(), ex.getMessage(), ex);
        }

    }

    public void sendVerificationEmail(String to, String verifyLink) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom("demo@gmail.com");
            messageHelper.setTo(to);
            messageHelper.setSubject("[CinePlex] Verify your email / Xác minh email của bạn");
            messageHelper.setText(
                    "<p>Please verify your email by clicking the link below (valid for 24 hours):</p>"
                            + "<p><a href=\"" + verifyLink + "\">Verify email</a></p>"
                            + "<p>If you did not register, ignore this email.</p>",
                    true);
        };

        try {
            mailSender.send(messagePreparator);
        } catch (MailException ex) {
            log.error("Exception occurred when sending verification email to {} with message: {}", to, ex.getMessage(), ex);
        }
    }

    public void sendReportEmail(String to, byte[] pdfBytes, String filename) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom("demo@gmail.com");
            messageHelper.setTo(to);
            messageHelper.setSubject("[CinePlex] Daily revenue report");
            messageHelper.setText("<p>Yesterday's revenue report is attached.</p>", true);
            messageHelper.addAttachment(filename, new ByteArrayResource(pdfBytes));
        };

        try {
            mailSender.send(messagePreparator);
        } catch (MailException ex) {
            log.error("Exception occurred when sending report email to {} with message: {}", to, ex.getMessage(), ex);
        }
    }

}

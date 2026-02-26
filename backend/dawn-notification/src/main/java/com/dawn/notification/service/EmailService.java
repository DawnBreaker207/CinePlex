package com.dawn.notification.service;

import com.dawn.common.core.dto.request.BookingNotificationEvent;
import com.dawn.common.core.utils.BarcodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    public void sendReservationEmail(BookingNotificationEvent event) {
        log.info("Got message from reservation");
        String barcodeBase64 = BarcodeUtils.generateCode128(event.getReservationId(), 300, 100);

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom("demo@gmail.com");
            messageHelper.setTo(event.getTo());
            messageHelper.setSubject("[Thông tin vé phim] - Đặt vé trực tuyến thành công / Your online ticket purchase has been successful");

            Context context = new Context();
            context.setVariable("name", event.getName());
            context.setVariable("reservationId", event.getReservationId());
            context.setVariable("movieName", event.getMovieName());
            context.setVariable("theaterName", event.getTheaterName());
            context.setVariable("showtimeSession", event.getShowtimeSession());
            context.setVariable("seats", event.getSeats());
            context.setVariable("paymentTime", event.getPaymentTime());
            context.setVariable("total", event.getTotal());
            context.setVariable("barcode", barcodeBase64);

            String html = templateEngine.process("email", context);
            messageHelper.setText(html, true);
        };

        try {
            mailSender.send(messagePreparator);
            log.info("Email notification sent!");
        } catch (MailException ex) {
            log.error("Exception occurred when sending email to {} with message: {}", event.getTo(),ex.getMessage());
        }

    }

}

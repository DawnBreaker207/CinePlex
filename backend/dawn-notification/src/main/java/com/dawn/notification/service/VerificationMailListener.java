package com.dawn.notification.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.EmailVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationMailListener {

    private final EmailService emailService;

    @Value("${service.url.frontend}")
    private String frontendUrl;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_NOTIFICATION_EMAIL_VERIFICATION)
    public void handleEmailVerification(EmailVerificationEvent event) {
        try {
            String link = frontendUrl + "/verify-email?token=" + event.token();
            emailService.sendVerificationEmail(event.to(), link);
            log.info("[Notification] Verification email sent to {}", event.to());
        } catch (Exception e) {
            log.error("[Notification] Failed to send verification email to {}: {}",
                    event.to(), e.getMessage(), e);
        }
    }
}

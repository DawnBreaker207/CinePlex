package com.dawn.payment.service;

import com.dawn.common.core.constant.RabbitMQConstants;
import com.dawn.common.core.dto.event.RefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefundRequestListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_PAYMENT_REFUND_REQUESTED)
    public void handleRefundRequested(RefundRequestedEvent event) {
        try {
            paymentService.markRefundPending(event.reservationCode());
        } catch (Exception e) {
            log.error("[Payment] Failed to mark refund pending for reservation {}: {}",
                    event.reservationCode(), e.getMessage(), e);
            throw e;
        }
    }
}

package com.dawn.payment.config.event;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitConfig {
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_PAYMENT, true, false);
    }

    @Bean
    public DirectExchange paymentDlx() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_PAYMENT_DLX, true, false);
    }

    @Bean
    public Queue paymentRefundRequestedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_PAYMENT_REFUND_REQUESTED)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key",
                        RabbitMQConstants.QUEUE_PAYMENT_REFUND_REQUESTED_DLQ)
                .build();
    }

    @Bean
    public Queue refundRequestedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_PAYMENT_REFUND_REQUESTED_DLQ).build();
    }

    @Bean
    public Binding paymentRefundRequestedBinding(Queue paymentRefundRequestedQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentRefundRequestedQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.RK_PAYMENT_REFUND_REQUESTED);
    }

    @Bean
    public Binding refundRequestedDlxBinding(Queue refundRequestedDlq, DirectExchange paymentDlx) {
        return BindingBuilder
                .bind(refundRequestedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.QUEUE_PAYMENT_REFUND_REQUESTED_DLQ);
    }

    // Original-rk DLQ binding for recoverer-republished messages.
    @Bean
    public Binding refundRequestedRecovererBinding(Queue refundRequestedDlq, DirectExchange paymentDlx) {
        return BindingBuilder
                .bind(refundRequestedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.RK_PAYMENT_REFUND_REQUESTED);
    }
}

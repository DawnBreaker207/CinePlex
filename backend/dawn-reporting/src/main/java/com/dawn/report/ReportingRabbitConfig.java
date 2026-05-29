package com.dawn.report;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ReportingRabbitConfig {
    @Bean
    public Queue reportingPaymentCompletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_REPORTING_PAYMENT_COMPLETED)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.QUEUE_REPORTING_PAYMENT_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Queue reportingPaymentCompletedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_REPORTING_PAYMENT_COMPLETED_DLQ).build();
    }

    @Bean
    public Binding reportingPaymentCompletedBinding(
            Queue reportingPaymentCompletedQueue,
            TopicExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(reportingPaymentCompletedQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.RK_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding reportingPaymentCompletedDlxBinding(
            Queue reportingPaymentCompletedDlq,
            DirectExchange paymentDlx
    ) {
        return BindingBuilder
                .bind(reportingPaymentCompletedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.QUEUE_REPORTING_PAYMENT_COMPLETED_DLQ);
    }
}

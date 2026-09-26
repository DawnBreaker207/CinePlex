package com.dawn.booking.config;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingRabbitConfig {
    // payment.completed
    @Bean
    public Queue bookingPaymentCompletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Queue bookingPaymentCompletedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED_DLQ).build();
    }

    @Bean
    public Binding bookingPaymentCompletedBinding(
            Queue bookingPaymentCompletedQueue,
            TopicExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(bookingPaymentCompletedQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.RK_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bookingPaymentCompletedDlxBinding(
            Queue bookingPaymentCompletedDlq,
            DirectExchange paymentDlx
    ) {
        return BindingBuilder
                .bind(bookingPaymentCompletedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_COMPLETED_DLQ);
    }

    // payment.failed
    @Bean
    public Queue bookingPaymentFailedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED_DLQ)
                .build();
    }

    @Bean
    public Queue bookingPaymentFailedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED_DLQ).build();
    }

    @Bean
    public Binding bookingPaymentFailedBinding(
            Queue bookingPaymentFailedQueue,
            TopicExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(bookingPaymentFailedQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.RK_PAYMENT_FAILED);
    }

    @Bean
    public Binding bookingPaymentFailedDlxBinding(
            Queue bookingPaymentFailedDlq,
            DirectExchange paymentDlx
    ) {
        return BindingBuilder
                .bind(bookingPaymentFailedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.QUEUE_BOOKING_PAYMENT_FAILED_DLQ);
    }

    // Original-rk DLQ bindings: RepublishMessageRecoverer re-sends exhausted
    // transient retries with the original routing key (see RabbitMQConfig).
    @Bean
    public Binding bookingPaymentCompletedRecovererBinding(
            Queue bookingPaymentCompletedDlq,
            DirectExchange paymentDlx
    ) {
        return BindingBuilder
                .bind(bookingPaymentCompletedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.RK_PAYMENT_COMPLETED);
    }

    @Bean
    public Binding bookingPaymentFailedRecovererBinding(
            Queue bookingPaymentFailedDlq,
            DirectExchange paymentDlx
    ) {
        return BindingBuilder
                .bind(bookingPaymentFailedDlq)
                .to(paymentDlx)
                .with(RabbitMQConstants.RK_PAYMENT_FAILED);
    }

    // cinema.showtime.canceled
    @Bean
    public TopicExchange cinemaExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_CINEMA);
    }

    @Bean
    public Queue bookingShowtimeCanceledQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_CINEMA_SHOWTIME_CANCELED).build();
    }

    @Bean
    public Binding bookingShowtimeCanceledBinding(
            Queue bookingShowtimeCanceledQueue,
            TopicExchange cinemaExchange
    ) {
        return BindingBuilder
                .bind(bookingShowtimeCanceledQueue)
                .to(cinemaExchange)
                .with(RabbitMQConstants.RK_CINEMA_SHOWTIME_CANCELED);
    }
}

package com.dawn.payment.config.event;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.DirectExchange;
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
}

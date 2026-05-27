package com.dawn.notification.config;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_NOTIFICATION);
    }

    @Bean
    public Queue queueNotify() {
        return new Queue(RabbitMQConstants.QUEUE_NOTIFICATION_RESERVATION_COMPLETED);
    }

    //
    @Bean
    public Binding bindingNotify(Queue queueNotify, TopicExchange exchange) {
        return BindingBuilder
                .bind(queueNotify)
                .to(exchange)
                .with(RabbitMQConstants.RK_NOTIFICATION_RESERVATION_COMPLETED);
    }

    //

    @Bean
    public Queue queueDashboard() {
        return new Queue(RabbitMQConstants.QUEUE_DASHBOARD);
    }

    @Bean
    public Binding bindingDashboard(Queue queueDashboard, TopicExchange exchange) {
        return BindingBuilder
                .bind(queueDashboard)
                .to(exchange)
                .with(RabbitMQConstants.RK_DASHBOARD_REFRESH);
    }
}

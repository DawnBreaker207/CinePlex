package com.dawn.notification.config;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue queueNotify() {
        return new Queue(RabbitMQConstants.QUEUE_NOTIFY);
    }

    @Bean
    public Queue queueDashboard(){
        return new Queue(RabbitMQConstants.QUEUE_DASHBOARD);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_NOTIFY);
    }

    @Bean
    public Binding bindingNotify(Queue queueNotify, TopicExchange exchange) {
        return BindingBuilder
                .bind(queueNotify)
                .to(exchange)
                .with(RabbitMQConstants.ROUTING_KEY_NOTIFY);
    }

    @Bean
    public Binding bindingDashboard(Queue queueDashboard, TopicExchange exchange) {
        return BindingBuilder
                .bind(queueDashboard)
                .to(exchange)
                .with(RabbitMQConstants.ROUTING_KEY_DASHBOARD);
    }

}

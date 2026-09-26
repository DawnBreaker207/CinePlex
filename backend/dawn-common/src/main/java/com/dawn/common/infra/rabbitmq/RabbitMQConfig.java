package com.dawn.common.infra.rabbitmq;

import com.dawn.common.core.constant.RabbitMQConstants;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    // S8: the queue/exchange/DLQ topology already lives in BookingRabbitConfig
    // and PaymentRabbitConfig (only the refund-queue DLX gap is closed there).
    // This file owns the transient-retry policy: 3 attempts with backoff, then
    // republish to the DLX. Permanent failures never reach here: listeners
    // ACK those paths themselves.
    //
    // RepublishMessageRecoverer re-sends with the ORIGINAL routing key, so the
    // DLQs get extra original-rk bindings (the existing DLQ-name bindings
    // serve the queue-level dead-letter path).
    @Bean
    public RetryTemplate paymentRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(new SimpleRetryPolicy(3));
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);
        template.setBackOffPolicy(backOff);
        return template;
    }

    @Bean
    public MessageRecoverer paymentMessageRecoverer(RabbitTemplate rabbitTemplate) {
        // Default error-routing prefix is "error." which matches no DLQ
        // binding (message silently dropped). Empty prefix republishes with
        // the original routing key, which the DLQs bind.
        RepublishMessageRecoverer recoverer =
                new RepublishMessageRecoverer(rabbitTemplate, RabbitMQConstants.EXCHANGE_PAYMENT_DLX);
        recoverer.setErrorRoutingKeyPrefix("");
        return recoverer;
    }

    // The original-rk DLQ bindings live next to the queues in
    // BookingRabbitConfig / PaymentRabbitConfig: this module must not
    // depend on Queue beans declared by other modules (breaks the
    // dawn-common test context which has no queues).

    @Bean
    public SimpleRabbitListenerContainerFactory paymentListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryTemplate paymentRetryTemplate,
            MessageRecoverer paymentMessageRecoverer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter());
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryOperations(paymentRetryTemplate)
                .recoverer(paymentMessageRecoverer)
                .build());
        return factory;
    }
}

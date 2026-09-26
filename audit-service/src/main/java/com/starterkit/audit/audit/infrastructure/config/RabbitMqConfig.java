package com.starterkit.audit.audit.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "starterkit.events";

    // Auth events
    public static final String AUTH_QUEUE = "audit.auth.events";
    public static final String AUTH_ROUTING = "auth.#";

    // Gateway events
    public static final String GATEWAY_QUEUE = "audit.gateway.events";
    public static final String GATEWAY_ROUTING = "gateway.#";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // ==================== AUTH QUEUE ====================

    @Bean
    public Queue auditAuthQueue() {
        return QueueBuilder.durable(AUTH_QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditAuthBinding(Queue auditAuthQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditAuthQueue)
                .to(eventsExchange)
                .with(AUTH_ROUTING);
    }

    // ==================== GATEWAY QUEUE ====================

    @Bean
    public Queue auditGatewayQueue() {
        return QueueBuilder.durable(GATEWAY_QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditGatewayBinding(Queue auditGatewayQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditGatewayQueue)
                .to(eventsExchange)
                .with(GATEWAY_ROUTING);
    }

    // ==================== CONVERTERS ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}

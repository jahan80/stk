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
    public static final String QUEUE = "audit.auth.events";
    public static final String ROUTING_KEY = "auth.#";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditQueue)
                .to(eventsExchange)
                .with(ROUTING_KEY);
    }

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

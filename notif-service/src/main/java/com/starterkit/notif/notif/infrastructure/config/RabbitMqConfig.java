package com.starterkit.notif.notif.infrastructure.config;

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
    public static final String NOTIF_QUEUE = "notif.events";
    public static final String NOTIF_ROUTING = "notif.#";

    // Dead Letter Exchange
    public static final String DLX_EXCHANGE = "starterkit.dlx";
    public static final String DLQ_QUEUE = "notif.dlq";
    public static final String DLQ_ROUTING = "notif.dead";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notifQueue() {
        return QueueBuilder.durable(NOTIF_QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "notif.dead")
                .build();
    }

    @Bean
    public Binding notifBinding(Queue notifQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notifQueue)
                .to(eventsExchange)
                .with(NOTIF_ROUTING);
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

    // ==================== DEAD LETTER EXCHANGE ====================

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue notifDlq() {
        // Bounded DLQ: cap at 10_000 messages and 7-day TTL so a stuck
        // consumer cannot fill the broker indefinitely.
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 7L * 24 * 60 * 60 * 1000)
                .withArgument("x-max-length", 10_000)
                .build();
    }

    @Bean
    public Binding notifDlqBinding(Queue notifDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notifDlq)
                .to(dlxExchange)
                .with(DLQ_ROUTING);
    }

}

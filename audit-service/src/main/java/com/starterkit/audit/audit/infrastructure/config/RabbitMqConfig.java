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

    // Ticket events
    public static final String TICKET_QUEUE = "audit.ticket.events";
    public static final String TICKET_ROUTING = "ticket.#";

    // Notif delivery events (Step 2)
    public static final String NOTIF_QUEUE = "audit.notif.events";
    public static final String NOTIF_ROUTING = "notif.#";

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

    // ==================== TICKET QUEUE ====================

    @Bean
    public Queue auditTicketQueue() {
        return QueueBuilder.durable(TICKET_QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditTicketBinding(Queue auditTicketQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditTicketQueue)
                .to(eventsExchange)
                .with(TICKET_ROUTING);
    }

    // ==================== NOTIF QUEUE ====================

    @Bean
    public Queue auditNotifQueue() {
        return QueueBuilder.durable(NOTIF_QUEUE)
                .withArgument("x-dead-letter-exchange", "starterkit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditNotifBinding(Queue auditNotifQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(auditNotifQueue)
                .to(eventsExchange)
                .with(NOTIF_ROUTING);
    }

    // ==================== DEAD LETTER ====================

    public static final String DLX_EXCHANGE = "starterkit.dlx";
    public static final String DLQ_QUEUE = "audit.dlq";
    public static final String DLQ_ROUTING = "audit.dead";

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 7L * 24 * 60 * 60 * 1000)
                .withArgument("x-max-length", 10_000)
                .build();
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(auditDlq)
                .to(dlxExchange)
                .with(DLQ_ROUTING);
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

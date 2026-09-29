package com.starterkit.ticket.ticket.infrastructure.config;

import com.starterkit.ticket.ticket.application.event.TicketEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventMessage;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqTicketEventPublisher implements TicketEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(TicketEvent event) {
        try {
            String traceId = MDC.get("traceId");
            TicketEventMessage message = TicketEventMessage.from(event, traceId);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    event.routingKey(),
                    message
            );

            log.info("Ticket event published: type={}, routingKey={}, eventId={}",
                    event.eventType(), event.routingKey(), message.getEventId());

        } catch (Exception ex) {
            log.error("Failed to publish ticket event: type={}", event.eventType(), ex);
        }
    }
}

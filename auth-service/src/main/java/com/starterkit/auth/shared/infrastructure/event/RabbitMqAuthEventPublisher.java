package com.starterkit.auth.shared.infrastructure.event;

import com.starterkit.auth.auth.application.event.AuthEvent;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.events.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RabbitMqAuthEventPublisher implements AuthEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(AuthEvent event) {
        try {
            String traceId = MDC.get("traceId");

            AuthEventMessage message = AuthEventMessage.from(event, traceId);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    event.routingKey(),
                    message
            );

            log.info("Event published: type={}, routingKey={}, eventId={}, traceId={}",
                    event.eventType(),
                    event.routingKey(),
                    message.getEventId(),
                    traceId);

        } catch (Exception ex) {
            // Don't break the main flow if event publishing fails
            log.error("Failed to publish event: type={}", event.eventType(), ex);
        }
    }
}

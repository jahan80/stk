package com.starterkit.auth.shared.infrastructure.event;

import com.starterkit.auth.auth.application.event.AuthEvent;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import com.starterkit.outbox.application.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publishes AuthEvent via Transactional Outbox Pattern.
 *
 * Instead of publishing directly to RabbitMQ, events are persisted
 * to auth.outbox_events within the same transaction as the business entity.
 * OutboxPublisher (scheduled) then publishes them to RabbitMQ.
 *
 * This guarantees at-least-once delivery even if RabbitMQ is down.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.events.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxAuthEventPublisher implements AuthEventPublisher {

    private final OutboxService outboxService;

    @Override
    public void publish(AuthEvent event) {
        try {
            Object userId = event.data().get("userId");
            String aggregateId = userId != null ? userId.toString() : "unknown";

            outboxService.save(
                    "User",
                    aggregateId,
                    event.eventType(),
                    event.routingKey(),
                    event.data()
            );

            log.debug("Auth event saved to outbox: type={}, aggregateId={}",
                    event.eventType(), aggregateId);

        } catch (Exception ex) {
            log.error("Failed to save auth event to outbox: type={}", event.eventType(), ex);
        }
    }
}

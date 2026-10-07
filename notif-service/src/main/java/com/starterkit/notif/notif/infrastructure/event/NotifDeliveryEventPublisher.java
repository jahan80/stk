package com.starterkit.notif.infrastructure.event;

import com.starterkit.notif.notif.application.event.NotifDeliveryEvent;
import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes delivery-result events to the events exchange.
 *
 * Best-effort: failures are logged but NOT propagated.
 * Rationale — audit/observability events must never break the
 * delivery path or roll back business state.
 *
 * The message shape matches the standard StarterKit envelope
 * consumed by audit-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifDeliveryEventPublisher {

    private static final String EXCHANGE = RabbitMqConfig.EXCHANGE;

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotifDeliveryEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventId", event.eventId().toString());
            message.put("eventType", event.eventType());
            message.put("eventVersion", "1.0");
            message.put("source", "notif-service");
            message.put("traceId", MDC.get("traceId"));
            message.put("data", event.data());
            message.put("occurredAt", event.occurredAt().toString());

            rabbitTemplate.convertAndSend(EXCHANGE, event.routingKey(), message);

            log.debug("Notif delivery event published: type={}, routingKey={}, notificationId={}",
                    event.eventType(), event.routingKey(), event.notificationId());

        } catch (Exception ex) {
            // Best-effort: do NOT propagate. A failed audit publish must
            // not break the delivery path or trigger a retry.
            log.warn("Failed to publish notif delivery event: type={}, notificationId={}, error={}",
                    event.eventType(), event.notificationId(), ex.getMessage());
        }
    }

    /** Convenience factory. */
    public NotifDeliveryEvent build(
            UUID sourceEventId,
            UUID notificationId,
            String outcome,
            String channel,
            String provider,
            String recipient,
            Integer attempts,
            String providerMessageId,
            String errorMessage
    ) {
        return new NotifDeliveryEvent(
                UUID.randomUUID(),
                sourceEventId,
                notificationId,
                outcome,
                channel,
                provider,
                recipient,
                attempts,
                providerMessageId,
                errorMessage,
                Instant.now()
        );
    }
}

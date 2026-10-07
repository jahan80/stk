package com.starterkit.gateway.event.publisher;

import com.starterkit.gateway.config.RabbitMqConfig;
import com.starterkit.gateway.event.GatewayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Synchronous gateway event publisher.
 *
 * Used only when gateway.events.publisher=sync (dev / debugging).
 * In production, prefer BoundedAsyncGatewayEventPublisher.
 *
 * This implementation publishes on the calling thread; if RabbitMQ
 * is unavailable, the event is lost and an error is logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "gateway.events.publisher",
        havingValue = "sync"
)
public class SyncGatewayEventPublisher implements GatewayEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(GatewayEvent event) {
        try {
            GatewayEventMessage message = GatewayEventMessage.from(event);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    event.routingKey(),
                    message
            );

            log.debug("Gateway event published (sync): type={}, routingKey={}",
                    event.eventType(), event.routingKey());

        } catch (Exception ex) {
            log.error("Failed to publish gateway event (sync): type={}",
                    event.eventType(), ex);
        }
    }
}

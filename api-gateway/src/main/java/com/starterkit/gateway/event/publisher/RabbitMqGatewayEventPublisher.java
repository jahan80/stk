package com.starterkit.gateway.event.publisher;

import com.starterkit.gateway.config.RabbitMqConfig;
import com.starterkit.gateway.event.GatewayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqGatewayEventPublisher implements GatewayEventPublisher {

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

            log.info("Gateway event published: type={}, routingKey={}, traceId={}",
                    event.eventType(),
                    event.routingKey(),
                    message.getTraceId());

        } catch (Exception ex) {
            log.error("Failed to publish gateway event: type={}", event.eventType(), ex);
        }
    }
}

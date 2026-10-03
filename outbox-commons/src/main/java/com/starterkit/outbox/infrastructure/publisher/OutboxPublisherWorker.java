package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.entity.OutboxStatus;
import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherWorker {

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties properties;

    @Transactional(readOnly = true)
    public List<Long> fetchPendingIds(int batchSize) {
        List<OutboxEvent> events = repository.findPendingForRetry(
                OutboxStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, batchSize)
        );
        return events.stream().map(OutboxEvent::getId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(Long eventId) {
        OutboxEvent event = repository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));

        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            log.debug("Outbox: event {} already published, skipping", eventId);
            return;
        }

        publishToRabbitMq(event);
        event.markPublished();
        repository.save(event);

        log.debug("Outbox: published eventId={}, type={}, routingKey={}",
                event.getEventId(), event.getEventType(), event.getRoutingKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long eventId, Exception ex) {
        repository.findById(eventId).ifPresent(event -> {
            event.markFailed(ex.getMessage());
            repository.save(event);

            log.warn("Outbox: failed eventId={}, type={}, attempts={}, error={}",
                    event.getEventId(), event.getEventType(), event.getAttempts(), ex.getMessage());
        });
    }

    private void publishToRabbitMq(OutboxEvent event) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", event.getEventId().toString());
        message.put("eventType", event.getEventType());
        message.put("eventVersion", "1.0");
        message.put("source", properties.getSourceService());
        message.put("traceId", event.getTraceId());
        message.put("data", event.getPayload());
        message.put("occurredAt", event.getCreatedAt().toString());

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                event.getRoutingKey(),
                message
        );
    }
}

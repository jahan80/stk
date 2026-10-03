package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.entity.OutboxStatus;
import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Worker for the Transactional Outbox Pattern.
 *
 * Design:
 *   1. fetchPendingIds()  - locks rows with SKIP LOCKED, returns IDs.
 *   2. publishOne(id)     - locks single row, sends to RabbitMQ with
 *                           publisher confirm, marks PUBLISHED.
 *
 * Both phases use short transactions. Long-running I/O (RabbitMQ)
 * happens inside the per-event transaction but the row is locked
 * for the entire duration to prevent double-publish.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherWorker {

    /** Max time to wait for RabbitMQ publisher confirm (per event). */
    private static final long PUBLISHER_CONFIRM_TIMEOUT_MS = 5_000L;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties properties;

    /**
     * Fetch pending event IDs and lock them for this publisher instance.
     *
     * Must run inside a transaction. The lock is held until the surrounding
     * transaction commits (i.e. until this method returns). Callers should
     * then immediately call publishOne() for each ID.
     */
    @Transactional
    public List<Long> fetchPendingIds(int batchSize) {
        List<OutboxEvent> events = repository.lockPendingForPublish(
                OutboxStatus.PENDING.name(),
                Instant.now(),
                batchSize
        );

        // NOTE: Returning IDs only; the DB lock is released when this
        // transaction commits. The actual publishing happens in publishOne(),
        // which re-acquires the lock. This is acceptable because publishOne()
        // uses SKIP LOCKED: if another instance grabbed the row in the
        // meantime, this instance will simply skip it.
        return events.stream().map(OutboxEvent::getId).toList();
    }

    /**
     * Publish a single event.
     *
     * Locks the row with SKIP LOCKED. If another instance already holds the
     * lock (or the row was already published), we skip silently.
     *
     * Sends to RabbitMQ with publisher confirm. Only marks PUBLISHED after
     * the broker confirms the message.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(Long eventId) {
        // 1. Lock the row (SKIP LOCKED: skip if another instance has it)
        OutboxEvent event = repository.lockById(eventId).orElse(null);

        if (event == null) {
            log.debug("Outbox: event {} not found or locked by another instance, skipping", eventId);
            return;
        }

        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            log.debug("Outbox: event {} already published, skipping", eventId);
            return;
        }

        // 2. Publish with publisher confirm
        try {
            publishToRabbitMqWithConfirm(event);
        } catch (Exception ex) {
            // Wrap in RuntimeException so it propagates out of @Transactional
            // (Spring rolls back on RuntimeException by default).
            log.warn("Outbox: publish failed for eventId={}, will be retried",
                    event.getEventId(), ex);
            throw new RuntimeException("Outbox publish failed for eventId="
                    + event.getEventId(), ex);
        }

        // 3. Broker confirmed - now safe to mark PUBLISHED
        event.markPublished();
        repository.save(event);

        log.debug("Outbox: published eventId={}, type={}, routingKey={}, attempts={}",
                event.getEventId(), event.getEventType(),
                event.getRoutingKey(), event.getAttempts());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long eventId, Exception ex) {
        repository.findById(eventId).ifPresent(event -> {
            event.markFailed(ex.getMessage());
            repository.save(event);

            log.warn("Outbox: failed eventId={}, type={}, attempts={}, error={}",
                    event.getEventId(), event.getEventType(),
                    event.getAttempts(), ex.getMessage());
        });
    }

    /**
     * Send message to RabbitMQ and wait for publisher confirm.
     *
     * Requires:
     *   spring.rabbitmq.publisher-confirm-type=correlated
     *   spring.rabbitmq.publisher-returns=true
     *   spring.rabbitmq.template.mandatory=true
     */
    private void publishToRabbitMqWithConfirm(OutboxEvent event) throws Exception {
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", event.getEventId().toString());
        message.put("eventType", event.getEventType());
        message.put("eventVersion", "1.0");
        message.put("source", properties.getSourceService());
        message.put("traceId", event.getTraceId());
        message.put("data", event.getPayload());
        message.put("occurredAt", event.getCreatedAt().toString());

        // Correlation ID lets us match the confirm to this specific message.
        CorrelationData correlationData = new CorrelationData(
                event.getEventId().toString()
        );

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                event.getRoutingKey(),
                message,
                correlationData
        );

        // Block until broker confirms (or nack / timeout).
        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(PUBLISHER_CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        if (confirm == null) {
            throw new IllegalStateException(
                    "RabbitMQ publisher confirm returned null for eventId=" + event.getEventId());
        }

        if (!confirm.isAck()) {
            throw new IllegalStateException(
                    "RabbitMQ nacked message for eventId=" + event.getEventId()
                            + ", reason=" + confirm.getReason());
        }

        // Also check for returned messages (unroutable).
        // With mandatory=true, if no queue is bound, we get a return.
        CorrelationData returned = correlationData;
        if (returned.getReturned() != null) {
            throw new IllegalStateException(
                    "RabbitMQ returned unroutable message for eventId=" + event.getEventId()
                            + ", replyText=" + returned.getReturned().getReplyText());
        }
    }
}

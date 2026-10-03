package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Multi-instance-safe worker for the Transactional Outbox Pattern.
 *
 * Design:
 *   1. claimBatch()  - atomically claims rows (PENDING → CLAIMED).
 *   2. publishOne()  - sends to RabbitMQ with publisher confirm.
 *   3. markPublished / markFailed  - transitions CLAIMED → PUBLISHED/PENDING.
 *
 * Claim safety:
 *   - Concurrent instances never see the same row because the claim uses
 *     FOR UPDATE SKIP LOCKED inside a single UPDATE.
 *   - If this instance crashes after claim, a reclaim job resets expired
 *     CLAIMED rows back to PENDING after locked_until.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherWorker {

    private static final long PUBLISHER_CONFIRM_TIMEOUT_MS = 5_000L;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxProperties properties;

    /**
     * Atomically claim a batch of PENDING events.
     * Returns the claimed events (already transitioned to CLAIMED).
     */
    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize) {
        return repository.claimBatch(
                properties.getInstanceId(),
                batchSize,
                properties.getLeaseSeconds()
        );
    }

    /**
     * Publish a single claimed event.
     * Only the instance that claimed the row should call this.
     * Runs in REQUIRES_NEW so a publish failure does not roll back
     * previously published events in the same batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishClaimed(OutboxEvent event) {
        // Re-load fresh to be safe within our transaction
        OutboxEvent fresh = repository
                .findByIdAndClaimedBy(event.getId(), properties.getInstanceId())
                .orElse(null);

        if (fresh == null) {
            log.debug("Outbox: event {} no longer claimed by us, skipping", event.getId());
            return;
        }

        try {
            publishToRabbitMqWithConfirm(fresh);
        } catch (Exception ex) {
            log.warn("Outbox: publish failed for eventId={}, will be retried",
                    fresh.getEventId(), ex);
            fresh.markFailed(ex.getMessage());
            repository.save(fresh);
            return;
        }

        fresh.markPublished();
        repository.save(fresh);

        log.debug("Outbox: published eventId={}, type={}, attempts={}",
                fresh.getEventId(), fresh.getEventType(), fresh.getAttempts());
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

        CorrelationData correlationData = new CorrelationData(
                event.getEventId().toString()
        );

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                event.getRoutingKey(),
                message,
                correlationData
        );

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

        if (correlationData.getReturned() != null) {
            throw new IllegalStateException(
                    "RabbitMQ returned unroutable message for eventId=" + event.getEventId()
                            + ", replyText=" + correlationData.getReturned().getReplyText());
        }
    }
}

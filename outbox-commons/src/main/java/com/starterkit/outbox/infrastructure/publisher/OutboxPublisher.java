package com.starterkit.outbox.infrastructure.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls outbox_events table and publishes pending events to RabbitMQ.
 *
 * Design: two-phase publishing (fetch -> per-event TX) to avoid:
 *   - Long transactions holding DB locks
 *   - Inconsistency when one event fails after others succeeded
 *
 * Each event is published in its own REQUIRES_NEW transaction
 * (delegated to OutboxPublisherWorker).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisher {

    private final OutboxPublisherWorker worker;
    private final OutboxPublisherProperties properties;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}")
    public void publishPendingEvents() {
        List<Long> pendingIds = worker.fetchPendingIds(properties.getBatchSize());

        if (pendingIds.isEmpty()) {
            return;
        }

        log.debug("Outbox: found {} pending event(s)", pendingIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Long id : pendingIds) {
            try {
                worker.publishOne(id);
                successCount++;
            } catch (Exception ex) {
                worker.markFailed(id, ex);
                failCount++;
            }
        }

        if (successCount > 0 || failCount > 0) {
            log.info("Outbox published batch: success={}, failed={}", successCount, failCount);
        }
    }
}

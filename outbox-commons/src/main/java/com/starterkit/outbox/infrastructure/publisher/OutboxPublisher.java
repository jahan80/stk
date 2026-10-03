package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls outbox_events and publishes pending events to RabbitMQ.
 *
 * Multi-instance safe:
 *   - claimBatch() atomically claims rows (PENDING → CLAIMED).
 *   - Each claimed row is only processed by the claiming instance.
 *   - A separate OutboxReclaimJob recovers expired claims.
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
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> claimed = worker.claimBatch(
                properties.getPublisher().getBatchSize()
        );

        if (claimed.isEmpty()) {
            return;
        }

        log.debug("Outbox: claimed {} event(s) for instance {}",
                claimed.size(), properties.getInstanceId());

        int successCount = 0;
        int failCount = 0;

        for (OutboxEvent event : claimed) {
            try {
                worker.publishClaimed(event);
                successCount++;
            } catch (Exception ex) {
                // publishClaimed already handles markFailed internally
                failCount++;
                log.error("Outbox: unexpected error for eventId={}",
                        event.getEventId(), ex);
            }
        }

        if (successCount > 0 || failCount > 0) {
            log.info("Outbox claimed batch: success={}, failed={}", successCount, failCount);
        }
    }
}

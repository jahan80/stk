package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.entity.OutboxStatus;
import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Deletes PUBLISHED events older than retention period.
 *
 * Runs every day at 3 AM UTC.
 *
 * Enabled by:
 *   outbox.enabled         (default: true)  - master switch
 *   outbox.retention-days  (default: 7)
 *
 * Note: cleanup is INDEPENDENT of the publisher. Even if the publisher is
 * disabled, we still want to clean up already-PUBLISHED events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxCleanupJob {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void cleanupPublishedEvents() {
        Instant cutoff = Instant.now()
                .minus(properties.getRetentionDays(), ChronoUnit.DAYS);

        int deleted = repository.deletePublishedOlderThan(OutboxStatus.PUBLISHED, cutoff);

        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} published events older than {}",
                    deleted, cutoff);
        }
    }
}

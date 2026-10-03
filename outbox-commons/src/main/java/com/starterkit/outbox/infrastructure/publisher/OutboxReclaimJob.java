package com.starterkit.outbox.infrastructure.publisher;

import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovers expired CLAIMED events back to PENDING.
 *
 * Scenario: a publisher instance claims a batch, then crashes before
 * publishing. Without this job, those rows would stay CLAIMED forever.
 *
 * Runs every outbox.publisher.reclaim-interval-ms (default: 60s).
 * Only reclaims rows whose locked_until < NOW().
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "outbox.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxReclaimJob {

    private final OutboxEventRepository repository;

    @Scheduled(fixedDelayString = "${outbox.publisher.reclaim-interval-ms:60000}")
    @Transactional
    public void reclaimExpiredClaims() {
        int reclaimed = repository.reclaimExpired();
        if (reclaimed > 0) {
            log.warn("Outbox reclaim: reset {} expired CLAIMED event(s) back to PENDING",
                    reclaimed);
        }
    }
}

package com.starterkit.notif.infrastructure.retry;

import com.starterkit.notif.notif.application.NotificationService;
import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retries FAILED / PENDING notifications.
 *
 * Multi-instance safe:
 *   - claimRetryBatch uses FOR UPDATE SKIP LOCKED.
 *   - Each row is claimed by exactly one instance.
 *   - If an instance crashes, a reclaim pass resets expired leases.
 *
 * Delivery is delegated back to NotificationService.retryDelivery(),
 * which re-invokes the provider for the SAME row (no new row).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notif.retry.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotifRetryJob {

    private final NotificationRepository repository;
    private final NotificationService notificationService;
    private final NotifRetryProperties props;

    @Scheduled(fixedDelayString = "${notif.retry.interval-ms:30000}")
    @Transactional
    public void reclaimExpiredLeases() {
        int reclaimed = repository.reclaimExpired();
        if (reclaimed > 0) {
            log.warn("Notif retry: reclaimed {} expired lease(s)", reclaimed);
        }
    }

    @Scheduled(fixedDelayString = "${notif.retry.interval-ms:30000}")
    public void retryFailedNotifications() {
        List<Notification> batch = repository.claimRetryBatch(
                props.getInstanceId(),
                props.getBatchSize(),
                props.getLeaseSeconds(),
                props.getMaxAttempts()
        );

        if (batch.isEmpty()) return;

        log.info("Notif retry: claimed {} row(s) for instance {}",
                batch.size(), props.getInstanceId());

        int ok = 0, fail = 0;
        for (Notification n : batch) {
            try {
                notificationService.retryDelivery(n.getId());
                ok++;
            } catch (Exception ex) {
                fail++;
                log.error("Notif retry: unexpected error for id={}", n.getId(), ex);
            }
        }

        if (ok > 0 || fail > 0) {
            log.info("Notif retry batch: ok={}, failed={}", ok, fail);
        }

        long exhausted = repository.countExhausted(props.getMaxAttempts());
        if (exhausted > 0) {
            log.warn("Notif retry: {} notification(s) exhausted max attempts — needs operator attention",
                    exhausted);
        }
    }
}

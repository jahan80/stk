package com.starterkit.notif.notif.application;

import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence layer for notifications.
 *
 * Each method runs in its OWN short transaction so that:
 *   - the PENDING row is committed BEFORE the external provider call,
 *   - the SENT/FAILED status is committed AFTER the provider call,
 *   - a provider exception does not roll back the initial PENDING row.
 *
 * After the retry fix (V3), FAILED rows are NOT duplicated on retry.
 * The same row is reused; attempts++ and next_attempt_at is pushed
 * forward. This makes provider-level retries actually work.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPersister {

    private final NotificationRepository repository;

    /**
     * Create a new notification row with status=PENDING.
     * Committed immediately so it survives provider failures.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createPending(
            UUID eventId,
            Notification.Channel channel,
            String recipient,
            String subject,
            String body,
            String provider,
            java.util.Map<String, Object> metadata
    ) {
        Notification n = new Notification();
        n.setNotificationId(UUID.randomUUID());
        n.setEventId(eventId);
        n.setChannel(channel);
        n.setRecipient(recipient);
        n.setSubject(subject);
        n.setBody(body);
        n.setProvider(provider);
        n.setStatus(Notification.Status.PENDING);
        n.setMetadata(metadata);
        n.setAttempts(0);
        n.setNextAttemptAt(Instant.now());

        Notification saved = repository.save(n);

        log.debug("Notification created PENDING: id={}, eventId={}, channel={}",
                saved.getId(), eventId, channel);

        return saved;
    }

    /**
     * Mark as SENT (terminal success). Short transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long notificationId, String providerMessageId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.markSent(providerMessageId);
            repository.save(n);
            log.debug("Notification marked SENT: id={}, providerMessageId={}",
                    notificationId, providerMessageId);
        });
    }

    /**
     * Mark as FAILED and schedule next retry. Short transaction.
     * The row is NOT deleted — it stays for the retry job.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(Long notificationId, String rawError, Instant nextAttemptAt) {
        repository.findById(notificationId).ifPresent(n -> {
            n.markRetry(rawError, nextAttemptAt);
            repository.save(n);
            log.warn("Notification FAILED (will retry): id={}, attempts={}, nextAttemptAt={}, error={}",
                    notificationId, n.getAttempts(), nextAttemptAt, rawError);
        });
    }

    /**
     * Load a notification in a fresh transaction.
     * Used by the retry job to read the row before delivering.
     */
    @Transactional(readOnly = true)
    public Notification load(Long notificationId) {
        return repository.findById(notificationId).orElse(null);
    }

    /**
     * Load a notification in a FRESH transaction, bypassing
     * the caller Hibernate session snapshot.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Notification loadFresh(Long id) {
        return repository.findById(id).orElse(null);
    }
}

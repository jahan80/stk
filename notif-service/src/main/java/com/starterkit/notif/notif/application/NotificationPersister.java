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
 * This decouples DB transactions from slow/blocking provider calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPersister {

    private final NotificationRepository repository;

    /**
     * Persist a new notification with status=PENDING.
     * Committed immediately so that it survives provider failures.
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

        Notification saved = repository.save(n);

        log.debug("Notification created PENDING: id={}, eventId={}, channel={}",
                saved.getId(), eventId, channel);

        return saved;
    }

    /**
     * Mark a notification as SENT. Short transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long notificationId, String providerMessageId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setStatus(Notification.Status.SENT);
            n.setProviderMessageId(providerMessageId);
            n.setSentAt(Instant.now());
            n.setErrorMessage(null);
            repository.save(n);

            log.debug("Notification marked SENT: id={}, providerMessageId={}",
                    notificationId, providerMessageId);
        });
    }

    /**
     * Mark a notification as FAILED. Short transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long notificationId, String errorMessage) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setStatus(Notification.Status.FAILED);
            n.setErrorMessage(errorMessage);
            repository.save(n);

            log.warn("Notification marked FAILED: id={}, error={}",
                    notificationId, errorMessage);
        });
    }
}

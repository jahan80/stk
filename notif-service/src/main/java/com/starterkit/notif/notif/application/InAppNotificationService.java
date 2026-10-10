package com.starterkit.notif.notif.application;

import com.starterkit.notif.notif.api.dto.NotificationResponse;
import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification (channel = IN_APP).
 *
 * These do NOT go through a provider: they are persisted with
 * status=SENT immediately and become visible to the user via
 * GET /notify/me.
 *
 * Why no outbox:
 *   - In-app is synchronous; if the DB write fails, the caller's
 *     business transaction rolls back, which is what we want.
 *   - There is no external provider to be eventually consistent with.
 *   - Volume is bounded (one row per recipient per event).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InAppNotificationService {

    private final NotificationRepository repository;
    private final NotificationPersister persister;

    /**
     * Persist one IN_APP notification. Called from the consumer of
     * NOTIFICATION_REQUESTED events with channel=IN_APP.
     */
    @Transactional
    public Notification createInApp(
            UUID sourceEventId,
            Long recipientUserId,
            String subject,
            String body,
            String link,
            Long ticketId,
            Long actorId
    ) {
        // Re-use persister for consistent createdAt/nextAttemptAt
        Notification n = persister.createPending(
                sourceEventId,
                Notification.Channel.IN_APP,
                String.valueOf(recipientUserId),   // legacy recipient column
                subject,
                body,
                "IN_APP",                          // provider label
                null                               // metadata
        );

        n.setRecipientUserId(recipientUserId);
        n.setLink(link);
        n.setTicketId(ticketId);
        n.setActorId(actorId);

        // IN_APP is immediately "delivered"
        n.markSent("in-app-" + n.getNotificationId());

        Notification saved = repository.save(n);

        log.debug("IN_APP notification created: id={}, userId={}, subject={}",
                saved.getId(), recipientUserId, subject);

        return saved;
    }

    // =====================================================
    // Queries (used by /notify/me endpoints)
    // =====================================================

    public Page<NotificationResponse> listForUser(Long userId, boolean unreadOnly, Pageable pageable) {
        return repository.findInAppForUser(userId, unreadOnly, pageable)
                .map(this::toResponse);
    }

    public long countUnread(Long userId) {
        return repository.countUnreadInApp(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        int updated = repository.markRead(notificationId, userId, Instant.now());
        if (updated == 0) {
            log.debug("markRead no-op for id={}, userId={}", notificationId, userId);
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .channel(n.getChannel().name())
                .recipient(n.getRecipient())
                .subject(n.getSubject())
                .status(n.getStatus().name())
                .provider(n.getProvider())
                .providerMessageId(n.getProviderMessageId())
                .errorMessage(n.getErrorMessage())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }

    @Transactional
    public void markReadByNotificationId(Long userId, UUID notificationId) {
        repository.findByNotificationId(notificationId).ifPresent(n -> {
            if (n.getRecipientUserId() != null
                    && n.getRecipientUserId().equals(userId)
                    && n.getChannel() == Notification.Channel.IN_APP
                    && n.getReadAt() == null) {
                n.markRead();
                repository.save(n);
            }
        });
    }
}

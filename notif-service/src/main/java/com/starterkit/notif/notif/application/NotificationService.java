package com.starterkit.notif.notif.application;

import com.starterkit.notif.notif.api.dto.EmailRequest;
import com.starterkit.notif.notif.api.dto.NotificationResponse;
import com.starterkit.notif.notif.api.dto.PushRequest;
import com.starterkit.notif.notif.api.dto.SmsRequest;
import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.notif.domain.provider.EmailProvider;
import com.starterkit.notif.notif.domain.provider.ProviderResponse;
import com.starterkit.notif.notif.domain.provider.PushProvider;
import com.starterkit.notif.notif.domain.provider.SmsProvider;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import com.starterkit.notif.infrastructure.event.NotifDeliveryEventPublisher;
import com.starterkit.notif.infrastructure.retry.NotifRetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Orchestrates notification delivery.
 *
 * After Step 2:
 *   - Every SENT / FAILED / EXHAUSTED outcome publishes a
 *     NotifDeliveryEvent (routing: notif.<channel>.<outcome>)
 *     which audit-service consumes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPersister persister;
    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;
    private final PushProvider pushProvider;
    private final NotifRetryProperties retryProps;
    private final NotifDeliveryEventPublisher deliveryEventPublisher;

    // =====================================================
    // First-time send (REST or MQ)
    // =====================================================

    public NotificationResponse sendSms(SmsRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId, Notification.Channel.SMS,
                request.getTo(), null, request.getMessage(),
                smsProvider.providerName(), request.getMetadata()
        );
        deliver(pending.getId(), () ->
                smsProvider.send(request.getTo(), request.getMessage()));
        return toResponse(reload(pending.getId()));
    }

    public NotificationResponse sendEmail(EmailRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId, Notification.Channel.EMAIL,
                request.getTo(), request.getSubject(), request.getBody(),
                emailProvider.providerName(), request.getMetadata()
        );
        deliver(pending.getId(), () ->
                emailProvider.send(request.getTo(), request.getSubject(), request.getBody()));
        return toResponse(reload(pending.getId()));
    }

    public NotificationResponse sendPush(PushRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId, Notification.Channel.PUSH,
                request.getDeviceToken(), request.getTitle(), request.getBody(),
                pushProvider.providerName(), request.getMetadata()
        );
        deliver(pending.getId(), () ->
                pushProvider.send(request.getDeviceToken(), request.getTitle(), request.getBody()));
        return toResponse(reload(pending.getId()));
    }

    // =====================================================
    // Retry entrypoint (called by NotifRetryJob)
    // =====================================================

    public void retryDelivery(Long notificationId) {
        Notification n = persister.load(notificationId);
        if (n == null) {
            log.warn("Notif retry: id={} disappeared, skipping", notificationId);
            return;
        }
        if (n.getStatus() == Notification.Status.SENT) {
            log.debug("Notif retry: id={} already SENT, skipping", notificationId);
            return;
        }

        log.info("Notif retry: attempting id={}, channel={}, attempt={}",
                notificationId, n.getChannel(), n.getAttempts() + 1);

        deliver(notificationId, () -> callProvider(n));
    }

    private ProviderResponse callProvider(Notification n) throws Exception {
        return switch (n.getChannel()) {
            case EMAIL -> emailProvider.send(n.getRecipient(), n.getSubject(), n.getBody());
            case SMS   -> smsProvider.send(n.getRecipient(), n.getBody());
            case PUSH  -> pushProvider.send(n.getRecipient(), n.getSubject(), n.getBody());
            case IN_APP -> throw new IllegalStateException(
                    "IN_APP notifications are synchronous and never go through the retry/provider path");
        };
    }

    // =====================================================
    // Delivery orchestration
    // =====================================================

    @FunctionalInterface
    private interface ProviderCall {
        ProviderResponse execute() throws Exception;
    }

    private void deliver(Long notificationId, ProviderCall call) {
        Notification current = persister.load(notificationId);
        if (current == null) {
            log.warn("Notif deliver: id={} disappeared, skipping", notificationId);
            return;
        }

        try {
            ProviderResponse response = call.execute();

            if (response.isSuccess()) {
                persister.markSent(notificationId, response.getProviderMessageId());

                deliveryEventPublisher.publish(deliveryEventPublisher.build(
                        current.getEventId(),
                        current.getNotificationId(),
                        "SENT",
                        current.getChannel().name(),
                        current.getProvider(),
                        current.getRecipient(),
                        current.getAttempts(),
                        response.getProviderMessageId(),
                        null
                ));
                return;
            }

            // Non-exception failure (e.g. SMTP returns ProviderResponse.failure)
            scheduleRetry(notificationId, response.getErrorMessage());

        } catch (Exception ex) {
            scheduleRetry(notificationId, ex.getMessage());
            log.error("Provider call failed for notification {}: {}",
                    notificationId, ex.getMessage(), ex);
        }
    }

    private void scheduleRetry(Long notificationId, String rawError) {
        Notification n = persister.load(notificationId);
        if (n == null) return;

        int nextAttempt = n.getAttempts() + 1;

        if (nextAttempt >= retryProps.getMaxAttempts()) {
            // Terminal: leave FAILED with next_attempt_at far in the future
            Instant terminal = Instant.now().plusSeconds(60L * 60 * 24 * 365); // 1 year
            persister.markRetry(notificationId, rawError + " [exhausted]", terminal);
            log.error("Notif id={} exhausted {} attempts, giving up",
                    notificationId, nextAttempt);

            deliveryEventPublisher.publish(deliveryEventPublisher.build(
                    n.getEventId(),
                    n.getNotificationId(),
                    "EXHAUSTED",
                    n.getChannel().name(),
                    n.getProvider(),
                    n.getRecipient(),
                    nextAttempt,
                    null,
                    rawError
            ));
            return;
        }

        Instant backoff = Instant.now().plusSeconds(backoffSeconds(nextAttempt));
        persister.markRetry(notificationId, rawError, backoff);

        deliveryEventPublisher.publish(deliveryEventPublisher.build(
                n.getEventId(),
                n.getNotificationId(),
                "FAILED",
                n.getChannel().name(),
                n.getProvider(),
                n.getRecipient(),
                nextAttempt,
                null,
                rawError
        ));
    }

    private long backoffSeconds(int attempt) {
        return switch (Math.min(attempt, 5)) {
            case 1 -> 30;
            case 2 -> 60;
            case 3 -> 300;
            case 4 -> 1800;
            default -> 7200;
        };
    }

    // =====================================================
    // Queries
    // =====================================================

    public Page<NotificationResponse> search(
            Notification.Channel channel,
            Notification.Status status,
            String recipient,
            Pageable pageable
    ) {
        String recipientFilter = (recipient != null && !recipient.isBlank())
                ? recipient.trim() : null;
        return notificationRepository
                .search(channel, status, recipientFilter, pageable)
                .map(this::toResponse);
    }

    public NotificationResponse getById(UUID notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId));
    }

    private Notification reload(Long id) {
        Notification fresh = persister.loadFresh(id);
        if (fresh == null) {
            throw new IllegalStateException(
                    "Notification disappeared after send: " + id);
        }
        return fresh;
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
}

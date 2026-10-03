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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates notification delivery.
 *
 * Flow for each send:
 *   1. Persist PENDING row (own short TX).
 *   2. Call provider OUTSIDE any transaction.
 *   3. Mark SENT or FAILED (own short TX).
 *   4. On provider exception: mark FAILED, then re-throw so the
 *      caller (RabbitMQ listener) can decide on retry / DLQ.
 *
 * This guarantees:
 *   - Provider failures are recorded (FAILED row).
 *   - A slow provider does not hold a DB transaction.
 *   - Duplicate events are handled by the listener (eventId check).
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

    // =====================================================
    // Public API: send (used by both REST and MQ consumer)
    // =====================================================

    public NotificationResponse sendSms(SmsRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId,
                Notification.Channel.SMS,
                request.getTo(),
                null,
                request.getMessage(),
                smsProvider.providerName(),
                request.getMetadata()
        );

        deliver(pending.getId(), () ->
                smsProvider.send(request.getTo(), request.getMessage())
        );

        return toResponse(reload(pending.getId()));
    }

    public NotificationResponse sendEmail(EmailRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId,
                Notification.Channel.EMAIL,
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                emailProvider.providerName(),
                request.getMetadata()
        );

        deliver(pending.getId(), () ->
                emailProvider.send(request.getTo(), request.getSubject(), request.getBody())
        );

        return toResponse(reload(pending.getId()));
    }

    public NotificationResponse sendPush(PushRequest request, UUID eventId) {
        Notification pending = persister.createPending(
                eventId,
                Notification.Channel.PUSH,
                request.getDeviceToken(),
                request.getTitle(),
                request.getBody(),
                pushProvider.providerName(),
                request.getMetadata()
        );

        deliver(pending.getId(), () ->
                pushProvider.send(request.getDeviceToken(), request.getTitle(), request.getBody())
        );

        return toResponse(reload(pending.getId()));
    }

    // =====================================================
    // Delivery orchestration (single source of truth)
    // =====================================================

    @FunctionalInterface
    private interface ProviderCall {
        ProviderResponse execute() throws Exception;
    }

    private void deliver(Long notificationId, ProviderCall call) {
        try {
            ProviderResponse response = call.execute();

            if (response.isSuccess()) {
                persister.markSent(notificationId, response.getProviderMessageId());
            } else {
                persister.markFailed(notificationId, response.getErrorMessage());
            }
        } catch (Exception ex) {
            // Provider threw — record failure, then re-throw so that the
            // RabbitMQ listener can decide on retry / DLQ.
            persister.markFailed(notificationId, ex.getMessage());
            log.error("Provider call failed for notification {}: {}",
                    notificationId, ex.getMessage(), ex);
            throw new RuntimeException(
                    "Provider call failed for notification " + notificationId, ex);
        }
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
        Page<Notification> notifications;

        if (channel != null) {
            notifications = notificationRepository.findByChannel(channel, pageable);
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(status, pageable);
        } else if (recipient != null && !recipient.isBlank()) {
            notifications = notificationRepository.findByRecipient(recipient, pageable);
        } else {
            notifications = notificationRepository.findAll(pageable);
        }

        return notifications.map(this::toResponse);
    }

    public NotificationResponse getById(UUID notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId));
    }

    // =====================================================
    // Helpers
    // =====================================================

    private Notification reload(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Notification disappeared after send: " + id));
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

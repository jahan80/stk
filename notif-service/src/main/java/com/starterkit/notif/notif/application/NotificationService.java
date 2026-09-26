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

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SmsProvider smsProvider;
    private final EmailProvider emailProvider;
    private final PushProvider pushProvider;

    @Transactional
    public NotificationResponse sendSms(SmsRequest request) {
        log.info("Sending SMS to {}", request.getTo());

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setChannel(Notification.Channel.SMS);
        notification.setRecipient(request.getTo());
        notification.setBody(request.getMessage());
        notification.setStatus(Notification.Status.PENDING);
        notification.setProvider(smsProvider.providerName());
        notification.setMetadata(request.getMetadata());

        // Send via provider
        ProviderResponse response = smsProvider.send(request.getTo(), request.getMessage());

        applyProviderResponse(notification, response);

        Notification saved = notificationRepository.save(notification);
        log.info("SMS notification saved: id={}, status={}", saved.getId(), saved.getStatus());

        return toResponse(saved);
    }

    @Transactional
    public NotificationResponse sendEmail(EmailRequest request) {
        log.info("Sending email to {}", request.getTo());

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setChannel(Notification.Channel.EMAIL);
        notification.setRecipient(request.getTo());
        notification.setSubject(request.getSubject());
        notification.setBody(request.getBody());
        notification.setStatus(Notification.Status.PENDING);
        notification.setProvider(emailProvider.providerName());
        notification.setMetadata(request.getMetadata());

        ProviderResponse response = emailProvider.send(
                request.getTo(),
                request.getSubject(),
                request.getBody()
        );

        applyProviderResponse(notification, response);

        Notification saved = notificationRepository.save(notification);
        log.info("Email notification saved: id={}, status={}", saved.getId(), saved.getStatus());

        return toResponse(saved);
    }

    @Transactional
    public NotificationResponse sendPush(PushRequest request) {
        log.info("Sending push to {}", request.getDeviceToken());

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setChannel(Notification.Channel.PUSH);
        notification.setRecipient(request.getDeviceToken());
        notification.setSubject(request.getTitle());
        notification.setBody(request.getBody());
        notification.setStatus(Notification.Status.PENDING);
        notification.setProvider(pushProvider.providerName());
        notification.setMetadata(request.getMetadata());

        ProviderResponse response = pushProvider.send(
                request.getDeviceToken(),
                request.getTitle(),
                request.getBody()
        );

        applyProviderResponse(notification, response);

        Notification saved = notificationRepository.save(notification);
        log.info("Push notification saved: id={}, status={}", saved.getId(), saved.getStatus());

        return toResponse(saved);
    }

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

    private void applyProviderResponse(Notification notification, ProviderResponse response) {
        if (response.isSuccess()) {
            notification.setStatus(Notification.Status.SENT);
            notification.setProviderMessageId(response.getProviderMessageId());
            notification.setSentAt(Instant.now());
        } else {
            notification.setStatus(Notification.Status.FAILED);
            notification.setErrorMessage(response.getErrorMessage());
        }
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

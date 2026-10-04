package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.shared.infrastructure.client.UserClient;
import com.starterkit.ticket.shared.infrastructure.client.UserContact;
import com.starterkit.ticket.ticket.application.event.NotifSendEmailEvent;
import com.starterkit.ticket.ticket.application.event.NotifSendSmsEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import com.starterkit.ticket.ticket.domain.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dispatches external notifications (email, sms) for ticket events.
 *
 * Design:
 *   - In-app notifications are handled separately (NotificationService).
 *   - This dispatcher only handles EXTERNAL channels.
 *   - Each channel is opt-in via ticket.configurations:
 *       TICKET.NOTIF.EMAIL.ENABLED
 *       TICKET.NOTIF.SMS.ENABLED
 *   - Commands go through the outbox (durable, at-least-once).
 *   - Failures (config read, user lookup) are logged; they do NOT
 *     roll back the business transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalNotificationDispatcher {

    private final TicketConfigurationService config;
    private final UserClient userClient;
    private final TicketEventPublisher eventPublisher;

    /**
     * Dispatch external notifications to the given user IDs.
     *
     * @param userIds  recipients (actor is expected to be filtered out by caller)
     * @param type     logical event (for logging / future per-event config)
     * @param title    short title (used as email subject, or sms prefix)
     * @param message  body text
     * @param relatedTicketId ticket id for correlation
     * @param actorId  user who triggered the event (never notified)
     */
    public void dispatch(Collection<Long> userIds,
                          NotificationType type,
                          String title,
                          String message,
                          Long relatedTicketId,
                          Long actorId) {

        if (userIds == null || userIds.isEmpty()) return;

        // dedup + remove actor
        Set<Long> recipients = new LinkedHashSet<>(userIds);
        recipients.remove(actorId);
        if (recipients.isEmpty()) return;

        boolean emailEnabled = isEnabled("TICKET.NOTIF.EMAIL.ENABLED");
        boolean smsEnabled   = isEnabled("TICKET.NOTIF.SMS.ENABLED");

        if (!emailEnabled && !smsEnabled) {
            log.debug("External notif: both email and sms disabled, skipping type={}", type);
            return;
        }

        for (Long userId : recipients) {
            UserContact contact = userClient.getContact(userId).orElse(null);
            if (contact == null) {
                log.debug("External notif: no contact info for userId={}", userId);
                continue;
            }

            if (emailEnabled && contact.getEmail() != null && !contact.getEmail().isBlank()) {
                enqueueEmail(contact.getEmail(), title, message, userId, type, relatedTicketId);
            }

            if (smsEnabled && contact.getMobileNumber() != null && !contact.getMobileNumber().isBlank()) {
                enqueueSms(contact.getMobileNumber(), title, message, userId, type, relatedTicketId);
            }
        }
    }

    private void enqueueEmail(String to, String subject, String body,
                               Long userId, NotificationType type, Long ticketId) {
        try {
            eventPublisher.publish(new NotifSendEmailEvent(to, subject, body, userId));
            log.debug("External notif enqueued: EMAIL to={}, type={}, ticketId={}",
                    to, type, ticketId);
        } catch (Exception ex) {
            // Publishing to outbox fails -> do NOT roll back business.
            // Log; the notification is lost (rare, since outbox uses same DB).
            log.error("Failed to enqueue EMAIL for userId={}, type={}", userId, type, ex);
        }
    }

    private void enqueueSms(String to, String title, String body,
                             Long userId, NotificationType type, Long ticketId) {
        try {
            String smsBody = (title != null && !title.isBlank())
                    ? title + "\n" + body
                    : body;
            eventPublisher.publish(new NotifSendSmsEvent(to, smsBody, userId));
            log.debug("External notif enqueued: SMS to={}, type={}, ticketId={}",
                    to, type, ticketId);
        } catch (Exception ex) {
            log.error("Failed to enqueue SMS for userId={}, type={}", userId, type, ex);
        }
    }

    private boolean isEnabled(String key) {
        try {
            return config.getBoolean(key, false);
        } catch (Exception ex) {
            log.warn("Failed to read config {}, defaulting to false", key);
            return false;
        }
    }
}

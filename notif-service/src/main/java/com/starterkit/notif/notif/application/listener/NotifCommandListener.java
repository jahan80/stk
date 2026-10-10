package com.starterkit.notif.notif.application.listener;

import com.starterkit.notif.notif.api.dto.NotifEventMessage;
import com.starterkit.notif.notif.application.InAppNotificationService;
import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes NOTIFICATION_REQUESTED events from other services.
 *
 * One event can target multiple recipients. Each recipient gets
 * its own row in notif.notifications.
 *
 * event_id uniqueness:
 *   notif.notifications.event_id is UNIQUE. When N recipients are
 *   notified from a single NOTIFICATION_REQUESTED event, we derive
 *   a per-recipient deterministic UUID:
 *
 *       UUID.nameUUIDFromBytes("<sourceEventId>:<recipientId>")
 *
 *   This is deterministic (same input -> same UUID), so:
 *     - The N rows have N distinct event_ids.
 *     - If the same event is redelivered (RabbitMQ at-least-once),
 *       the derived UUIDs collide with the existing rows and the
 *       UNIQUE constraint blocks duplicates — exactly what we want.
 *
 * Channels (D2 scope):
 *   IN_APP -> InAppNotificationService.createInApp()
 *   EMAIL  -> not yet handled here (still via SEND_EMAIL direct events)
 *   SMS    -> not yet handled here (still via SEND_SMS direct events)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifCommandListener {

    private final InAppNotificationService inAppService;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void onNotifRequested(NotifEventMessage message) {

        String eventType = message.getEventType();

        if (!"NOTIFICATION_REQUESTED".equals(eventType)) {
            return;
        }

        UUID sourceEventId;
        try {
            sourceEventId = UUID.fromString(message.getEventId());
        } catch (Exception ex) {
            log.error("Invalid eventId in NOTIFICATION_REQUESTED: {}", message.getEventId(), ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid eventId: " + message.getEventId(), ex);
        }

        Map<String, Object> data = message.getData();
        if (data == null) {
            throw new AmqpRejectAndDontRequeueException(
                    "Missing data for NOTIFICATION_REQUESTED, eventId=" + sourceEventId);
        }

        List<?> recipientIdsRaw = (List<?>) data.get("recipientUserIds");
        List<?> channelsRaw = (List<?>) data.get("channels");

        if (recipientIdsRaw == null || recipientIdsRaw.isEmpty()) {
            log.warn("NOTIFICATION_REQUESTED has no recipients: eventId={}", sourceEventId);
            return;
        }
        if (channelsRaw == null || channelsRaw.isEmpty()) {
            log.warn("NOTIFICATION_REQUESTED has no channels: eventId={}", sourceEventId);
            return;
        }

        String title = asString(data.get("title"));
        String body = asString(data.get("message"));
        String link = asString(data.get("link"));
        Long ticketId = asLong(data.get("referenceId"));
        Long actorId = asLong(data.get("actorId"));
        String type = asString(data.get("type"));

        boolean inAppRequested = channelsRaw.stream()
                .map(String::valueOf)
                .anyMatch("IN_APP"::equalsIgnoreCase);

        if (!inAppRequested) {
            log.debug("NOTIFICATION_REQUESTED does not include IN_APP: eventId={}", sourceEventId);
            return;
        }

        for (Object rawId : recipientIdsRaw) {
            Long recipientId = asLong(rawId);
            if (recipientId == null) continue;

            UUID perRecipientEventId = deriveEventId(sourceEventId, recipientId);

            try {
                inAppService.createInApp(
                        perRecipientEventId,
                        recipientId,
                        title,
                        body,
                        link,
                        ticketId,
                        actorId
                );
                log.debug("IN_APP notification created: sourceEventId={}, recipientId={}, type={}",
                        sourceEventId, recipientId, type);
            } catch (Exception ex) {
                if (isUniqueConstraintViolation(ex)) {
                    log.info("Duplicate IN_APP for sourceEventId={}, recipientId={}, skipping",
                            sourceEventId, recipientId);
                    continue;
                }
                log.error("Failed to create IN_APP for sourceEventId={}, recipientId={}",
                        sourceEventId, recipientId, ex);
                throw ex;
            }
        }
    }

    /**
     * Deterministic UUID for (source event, recipient).
     */
    private static UUID deriveEventId(UUID sourceEventId, Long recipientId) {
        String seed = sourceEventId.toString() + ":" + recipientId;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isUniqueConstraintViolation(Exception ex) {
        Throwable t = ex;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("uk_notifications_event_id")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private String asString(Object v) { return v != null ? v.toString() : null; }

    private Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}

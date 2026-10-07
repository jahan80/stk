package com.starterkit.notif.notif.application.listener;

import com.starterkit.notif.notif.api.dto.EmailRequest;
import com.starterkit.notif.notif.api.dto.NotifEventMessage;
import com.starterkit.notif.notif.api.dto.PushRequest;
import com.starterkit.notif.notif.api.dto.SmsRequest;
import com.starterkit.notif.notif.application.NotificationService;
import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumes notification COMMAND events (SEND_EMAIL / SEND_SMS / SEND_PUSH)
 * from the notif.events queue.
 *
 * IMPORTANT — self-loop protection:
 *   notif-service itself publishes RESULT events (NOTIFICATION_SENT /
 *   NOTIFICATION_FAILED / NOTIFICATION_EXHAUSTED) with routing key
 *   "notif.<channel>.<outcome>". Those events also match the notif.#
 *   binding and would come back to THIS queue.
 *
 *   We must ACK them without processing:
 *     - They are for audit-service, not for us.
 *     - If we throw AmqpRejectAndDontRequeueException, they go to DLQ
 *       and eventually fill it.
 *
 * Idempotency policy (unchanged):
 *   SENT      → ACK, skip (already delivered)
 *   PENDING   → ACK, skip (in-flight)
 *   FAILED    → ACK, skip (retry job owns it)
 *   absent    → first delivery, process now
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventListener {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void onNotifEvent(NotifEventMessage message) {

        String eventType = message.getEventType();

        // ===== SELF-LOOP PROTECTION =====
        // Our own result events (NOTIFICATION_*) have routing keys that
        // also match "notif.#". Skip them silently.
        if (eventType != null && eventType.startsWith("NOTIFICATION_")) {
            log.debug("Ignoring own result event: type={}, eventId={}",
                    eventType, message.getEventId());
            return;
        }

        log.info("Received notif event: type={}, eventId={}, source={}, traceId={}",
                eventType, message.getEventId(),
                message.getSource(), message.getTraceId());

        UUID eventId;
        try {
            eventId = UUID.fromString(message.getEventId());
        } catch (Exception ex) {
            log.error("Invalid eventId, rejecting to DLQ: {}", message.getEventId(), ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid eventId: " + message.getEventId(), ex);
        }

        if (eventType == null || eventType.isBlank()) {
            log.error("Missing event type, rejecting to DLQ: eventId={}", eventId);
            throw new AmqpRejectAndDontRequeueException(
                    "Missing event type for eventId: " + eventId);
        }

        // ===== Status-aware idempotency =====
        Optional<Notification> existing = notificationRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            Notification n = existing.get();
            log.info("Duplicate event (status={}), ACKing: eventId={}, notificationId={}",
                    n.getStatus(), eventId, n.getNotificationId());
            return;
        }

        try {
            switch (eventType) {
                case "SEND_SMS"   -> handleSms(message, eventId);
                case "SEND_EMAIL" -> handleEmail(message, eventId);
                case "SEND_PUSH"  -> handlePush(message, eventId);
                default -> {
                    log.error("Unknown COMMAND event type: {} (eventId={}), rejecting to DLQ",
                            eventType, eventId);
                    throw new AmqpRejectAndDontRequeueException(
                            "Unknown event type: " + eventType);
                }
            }
        } catch (AmqpRejectAndDontRequeueException ex) {
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("uk_notifications_event_id")) {
                log.info("Duplicate event (race), ACKing: eventId={}", eventId);
                return;
            }
            log.error("Unexpected integrity violation for eventId={}", eventId, ex);
            throw ex;
        } catch (Exception ex) {
            log.warn("Delivery failed for eventId={}; retry scheduled. {}",
                    eventId, ex.getMessage());
        }
    }

    private void handleSms(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = requireData(message, eventId, "SEND_SMS");
        SmsRequest req = new SmsRequest();
        req.setTo(asString(data.get("to")));
        req.setMessage(asString(data.get("message")));
        notificationService.sendSms(req, eventId);
    }

    private void handleEmail(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = requireData(message, eventId, "SEND_EMAIL");
        EmailRequest req = new EmailRequest();
        req.setTo(asString(data.get("to")));
        req.setSubject(asString(data.get("subject")));
        req.setBody(asString(data.get("body")));
        notificationService.sendEmail(req, eventId);
    }

    private void handlePush(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = requireData(message, eventId, "SEND_PUSH");
        PushRequest req = new PushRequest();
        req.setDeviceToken(asString(data.get("deviceToken")));
        req.setTitle(asString(data.get("title")));
        req.setBody(asString(data.get("body")));
        notificationService.sendPush(req, eventId);
    }

    private Map<String, Object> requireData(NotifEventMessage m, UUID eventId, String type) {
        Map<String, Object> d = m.getData();
        if (d == null) {
            throw new AmqpRejectAndDontRequeueException(
                    "Missing data payload for " + type + ", eventId=" + eventId);
        }
        return d;
    }

    private String asString(Object v) { return v != null ? v.toString() : null; }
}

package com.starterkit.notif.notif.application.listener;

import com.starterkit.notif.notif.api.dto.EmailRequest;
import com.starterkit.notif.notif.api.dto.NotifEventMessage;
import com.starterkit.notif.notif.api.dto.PushRequest;
import com.starterkit.notif.notif.api.dto.SmsRequest;
import com.starterkit.notif.notif.application.NotificationService;
import com.starterkit.notif.notif.domain.repository.NotificationRepository;
import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes notification events from the notif.events queue.
 *
 * Exception policy:
 *   - AmqpRejectAndDontRequeueException → straight to DLQ (no retry)
 *     Used for: malformed messages, unknown event types, invalid event IDs.
 *   - Other exceptions → requeue with Spring AMQP retry (up to 3 attempts)
 *     Used for: transient failures (DB down, provider timeout).
 *   - After max retries → DLX → DLQ (bounded: 7d TTL, 10k max).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventListener {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void onNotifEvent(NotifEventMessage message) {

        log.info("Received notif event: type={}, eventId={}, source={}, traceId={}",
                message.getEventType(),
                message.getEventId(),
                message.getSource(),
                message.getTraceId());

        // ===== Validate event ID =====
        UUID eventId;
        try {
            eventId = UUID.fromString(message.getEventId());
        } catch (Exception ex) {
            log.error("Invalid eventId in message, rejecting to DLQ: {}", message.getEventId(), ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid eventId: " + message.getEventId(), ex);
        }

        // ===== Validate event type =====
        if (message.getEventType() == null || message.getEventType().isBlank()) {
            log.error("Missing event type, rejecting to DLQ: eventId={}", eventId);
            throw new AmqpRejectAndDontRequeueException(
                    "Missing event type for eventId: " + eventId);
        }

        // ===== Idempotency guard =====
        // Fast pre-check for the common duplicate-delivery case.
        // The UNIQUE constraint on event_id is the ultimate guard; if two
        // consumers race, one will win and the other gets a constraint
        // violation which we treat as "already processed" (see catch below).
        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, skipping: eventId={}, type={}",
                    eventId, message.getEventType());
            return;
        }

        // ===== Dispatch by type =====
        try {
            switch (message.getEventType()) {
                case "SEND_SMS" -> handleSms(message, eventId);
                case "SEND_EMAIL" -> handleEmail(message, eventId);
                case "SEND_PUSH" -> handlePush(message, eventId);
                default -> {
                    // Unknown event type: this is a producer bug.
                    // Do NOT silently ACK — route to DLQ so ops can inspect.
                    log.error("Unknown notif event type: {} (eventId={}), rejecting to DLQ",
                            message.getEventType(), eventId);
                    throw new AmqpRejectAndDontRequeueException(
                            "Unknown notif event type: " + message.getEventType());
                }
            }
        } catch (AmqpRejectAndDontRequeueException ex) {
            // Already classified as "do not retry" — let it propagate to DLQ.
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process notif event: eventId={}, type={}",
                    eventId, message.getEventType(), ex);
            // Re-throw so RabbitMQ retries (or routes to DLQ after max attempts).
            throw ex;
        }
    }

    private void handleSms(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = message.getData();

        SmsRequest request = new SmsRequest();
        request.setTo(asString(data.get("to")));
        request.setMessage(asString(data.get("message")));

        notificationService.sendSms(request, eventId);
    }

    private void handleEmail(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = message.getData();

        EmailRequest request = new EmailRequest();
        request.setTo(asString(data.get("to")));
        request.setSubject(asString(data.get("subject")));
        request.setBody(asString(data.get("body")));

        notificationService.sendEmail(request, eventId);
    }

    private void handlePush(NotifEventMessage message, UUID eventId) {
        Map<String, Object> data = message.getData();

        PushRequest request = new PushRequest();
        request.setDeviceToken(asString(data.get("deviceToken")));
        request.setTitle(asString(data.get("title")));
        request.setBody(asString(data.get("body")));

        notificationService.sendPush(request, eventId);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

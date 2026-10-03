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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

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

        UUID eventId;
        try {
            eventId = UUID.fromString(message.getEventId());
        } catch (Exception ex) {
            log.error("Invalid eventId in message, rejecting: {}", message.getEventId(), ex);
            throw new IllegalArgumentException("Invalid eventId: " + message.getEventId());
        }

        // Idempotency guard: skip if this event has already been processed.
        // The UNIQUE constraint on event_id is the ultimate guard; this is
        // a fast pre-check for the common case (duplicate delivery).
        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, skipping: eventId={}, type={}",
                    eventId, message.getEventType());
            return;
        }

        try {
            switch (message.getEventType()) {
                case "SEND_SMS" -> handleSms(message, eventId);
                case "SEND_EMAIL" -> handleEmail(message, eventId);
                case "SEND_PUSH" -> handlePush(message, eventId);
                default -> {
                    log.warn("Unknown notif event type: {} (eventId={})",
                            message.getEventType(), eventId);
                    // Unknown event type: don't retry. Could route to DLQ,
                    // but for now we just log and ACK.
                }
            }
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

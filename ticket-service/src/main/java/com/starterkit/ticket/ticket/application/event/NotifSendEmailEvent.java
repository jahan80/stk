package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification command persisted to outbox.
 *
 * Routing: notif.email.send → notif-service
 *
 * Goes through the transactional outbox so it is delivered
 * at-least-once, even if RabbitMQ is temporarily unavailable.
 */
public record NotifSendEmailEvent(
        String to,
        String subject,
        String body,
        Long relatedUserId
) implements TicketEvent {

    @Override public String eventType() { return "SEND_EMAIL"; }
    @Override public String routingKey() { return "notif.email.send"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("to", to);
        d.put("subject", subject);
        d.put("body", body);
        if (relatedUserId != null) {
            d.put("relatedUserId", relatedUserId);
        }
        return d;
    }
}

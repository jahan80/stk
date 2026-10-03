package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification command persisted to outbox.
 *
 * Routing: notif.email.send → notif-service
 *
 * Unlike NotifEventPublisher (which is best-effort), this event goes
 * through the outbox so it is delivered at-least-once.
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

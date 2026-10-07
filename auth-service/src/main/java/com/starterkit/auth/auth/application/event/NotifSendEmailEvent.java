package com.starterkit.auth.auth.application.event;

import java.util.HashMap;
import java.util.Map;

/**
 * FIX P1-5 — notification command persisted to auth.outbox_events.
 * Routing: notif.email.send → notif-service.
 */
public record NotifSendEmailEvent(
        String to,
        String subject,
        String body,
        Long relatedUserId
) implements AuthEvent {

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

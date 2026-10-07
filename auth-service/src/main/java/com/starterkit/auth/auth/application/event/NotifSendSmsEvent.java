package com.starterkit.auth.auth.application.event;

import java.util.HashMap;
import java.util.Map;

/**
 * FIX P1-5 — SMS notification command persisted to auth.outbox_events.
 * Routing: notif.sms.send → notif-service.
 */
public record NotifSendSmsEvent(
        String to,
        String message,
        Long relatedUserId
) implements AuthEvent {

    @Override public String eventType() { return "SEND_SMS"; }
    @Override public String routingKey() { return "notif.sms.send"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("to", to);
        d.put("message", message);
        if (relatedUserId != null) {
            d.put("relatedUserId", relatedUserId);
        }
        return d;
    }
}

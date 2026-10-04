package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

/**
 * SMS notification command persisted to outbox.
 * Routing: notif.sms.send → notif-service
 */
public record NotifSendSmsEvent(
        String to,
        String message,
        Long relatedUserId
) implements TicketEvent {

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

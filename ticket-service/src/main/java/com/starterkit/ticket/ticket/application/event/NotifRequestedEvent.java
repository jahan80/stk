package com.starterkit.ticket.ticket.application.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single command event for ALL notification channels.
 *
 * Replaces:
 *   - ticket.notifications DB writes (in-app)
 *   - ExternalNotificationDispatcher direct outbox events per channel
 *
 * Routing: notif.requested
 * Consumer: notif-service NotifCommandListener
 *
 * Notif-service decides how to deliver per channel:
 *   - IN_APP  -> InAppNotificationService.createInApp()
 *   - EMAIL   -> NotificationService.sendEmail() with recipient email lookup
 *   - SMS     -> NotificationService.sendSms() with recipient phone lookup
 *
 * The ticket domain must NOT know how email/SMS are resolved:
 * it just says "these users should get this message on these channels".
 */
public record NotifRequestedEvent(
        List<Long> recipientUserIds,
        Long actorId,
        String type,
        String title,
        String message,
        List<String> channels,
        String referenceType,
        Long referenceId,
        String link
) implements TicketEvent {

    @Override public String eventType() { return "NOTIFICATION_REQUESTED"; }
    @Override public String routingKey() { return "notif.requested"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("recipientUserIds", new ArrayList<>(recipientUserIds));
        if (actorId != null) d.put("actorId", actorId);
        d.put("type", type);
        d.put("title", title);
        d.put("message", message);
        d.put("channels", new ArrayList<>(channels));
        if (referenceType != null) d.put("referenceType", referenceType);
        if (referenceId != null) d.put("referenceId", referenceId);
        if (link != null) d.put("link", link);
        return d;
    }
}

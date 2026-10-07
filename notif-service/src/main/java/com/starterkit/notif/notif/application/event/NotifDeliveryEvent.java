package com.starterkit.notif.notif.application.event;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Delivery result event emitted by notif-service.
 *
 * Routing keys:
 *   notif.email.sent, notif.email.failed, notif.email.exhausted
 *   notif.sms.sent,   notif.sms.failed,   notif.sms.exhausted
 *   notif.push.sent,  notif.push.failed,  notif.push.exhausted
 *
 * Consumed by audit-service to record the actual delivery outcome.
 *
 * NOTE: this is a non-business observability event. We do NOT put it
 * through the transactional outbox — if an audit row is lost, we lose
 * a log line, not business state. A best-effort publish with retry is
 * sufficient.
 */
public record NotifDeliveryEvent(
        UUID eventId,             // unique id for THIS delivery event
        UUID sourceEventId,       // the original event that triggered notif
        UUID notificationId,      // notif.notifications.notification_id
        String outcome,           // SENT | FAILED | EXHAUSTED
        String channel,           // EMAIL | SMS | PUSH
        String provider,          // MOCK | SMTP | KAVENEGAR | ...
        String recipient,
        Integer attempts,
        String providerMessageId,
        String errorMessage,
        Instant occurredAt
) {

    public String eventType() {
        return "NOTIFICATION_" + outcome;
    }

    public String routingKey() {
        return "notif." + channel.toLowerCase() + "." + outcome.toLowerCase();
    }

    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("sourceEventId", sourceEventId != null ? sourceEventId.toString() : null);
        d.put("notificationId", notificationId != null ? notificationId.toString() : null);
        d.put("outcome", outcome);
        d.put("channel", channel);
        d.put("provider", provider);
        d.put("recipient", maskRecipient(recipient));
        d.put("attempts", attempts);
        if (providerMessageId != null) d.put("providerMessageId", providerMessageId);
        if (errorMessage != null) d.put("errorMessage", truncate(errorMessage, 500));
        return d;
    }

    /** Mask recipient for audit privacy (keep domain for email, last 4 for phone). */
    private static String maskRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) return recipient;
        if (recipient.contains("@")) {
            int at = recipient.indexOf('@');
            String local = recipient.substring(0, at);
            String domain = recipient.substring(at);
            if (local.length() <= 2) return "***" + domain;
            return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
        }
        // phone: keep last 4 digits
        if (recipient.length() <= 4) return "***";
        return "***" + recipient.substring(recipient.length() - 4);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}

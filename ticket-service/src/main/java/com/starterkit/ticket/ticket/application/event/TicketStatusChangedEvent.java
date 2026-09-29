package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

public record TicketStatusChangedEvent(
        Long ticketId,
        String ticketNumber,
        String oldStatus,
        String newStatus,
        Long changedBy,
        Long ticketOwnerId
) implements TicketEvent {

    @Override public String eventType() { return "TICKET_STATUS_CHANGED"; }
    @Override public String routingKey() { return "ticket.status-changed"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("ticketId", ticketId);
        d.put("ticketNumber", ticketNumber);
        d.put("oldStatus", oldStatus);
        d.put("newStatus", newStatus);
        d.put("changedBy", changedBy);
        d.put("ticketOwnerId", ticketOwnerId);
        return d;
    }
}

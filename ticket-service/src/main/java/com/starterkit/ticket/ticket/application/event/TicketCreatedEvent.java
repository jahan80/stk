package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

public record TicketCreatedEvent(
        Long ticketId,
        String ticketNumber,
        String title,
        Long createdBy,
        String createdByEmail,
        String priority,
        String categoryName
) implements TicketEvent {

    @Override public String eventType() { return "TICKET_CREATED"; }
    @Override public String routingKey() { return "ticket.created"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("ticketId", ticketId);
        d.put("ticketNumber", ticketNumber);
        d.put("title", title);
        d.put("createdBy", createdBy);
        d.put("createdByEmail", createdByEmail);
        d.put("priority", priority);
        d.put("categoryName", categoryName);
        return d;
    }
}

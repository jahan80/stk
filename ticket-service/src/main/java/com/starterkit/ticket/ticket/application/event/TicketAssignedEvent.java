package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

public record TicketAssignedEvent(
        Long ticketId,
        String ticketNumber,
        Long assignedTo,
        Long assignedBy
) implements TicketEvent {

    @Override public String eventType() { return "TICKET_ASSIGNED"; }
    @Override public String routingKey() { return "ticket.assigned"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("ticketId", ticketId);
        d.put("ticketNumber", ticketNumber);
        d.put("assignedTo", assignedTo);
        d.put("assignedBy", assignedBy);
        return d;
    }
}

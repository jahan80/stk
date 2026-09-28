package com.starterkit.ticket.ticket.application.event;

import java.util.HashMap;
import java.util.Map;

public record TicketCommentedEvent(
        Long ticketId,
        String ticketNumber,
        Long commentId,
        Long authorId,
        String authorRole,
        Long ticketOwnerId,
        Long assignedTo
) implements TicketEvent {

    @Override public String eventType() { return "TICKET_COMMENTED"; }
    @Override public String routingKey() { return "ticket.commented"; }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("ticketId", ticketId);
        d.put("ticketNumber", ticketNumber);
        d.put("commentId", commentId);
        d.put("authorId", authorId);
        d.put("authorRole", authorRole);
        d.put("ticketOwnerId", ticketOwnerId);
        d.put("assignedTo", assignedTo);
        return d;
    }
}

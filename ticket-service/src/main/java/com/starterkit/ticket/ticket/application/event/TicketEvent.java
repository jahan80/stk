package com.starterkit.ticket.ticket.application.event;

import java.util.Map;

public interface TicketEvent {
    String eventType();
    String routingKey();
    Map<String, Object> data();
}

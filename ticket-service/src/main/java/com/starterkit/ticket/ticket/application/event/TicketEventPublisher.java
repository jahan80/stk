package com.starterkit.ticket.ticket.application.event;

public interface TicketEventPublisher {
    void publish(TicketEvent event);
}

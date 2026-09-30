package com.starterkit.ticket.ticket.infrastructure.config;

import com.starterkit.outbox.application.OutboxService;
import com.starterkit.ticket.ticket.application.event.TicketEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.events.publisher",
        havingValue = "outbox",
        matchIfMissing = true
)
public class OutboxTicketEventPublisher implements TicketEventPublisher {

    private final OutboxService outboxService;

    @Override
    public void publish(TicketEvent event) {
        try {
            Object ticketId = event.data().get("ticketId");
            String aggregateId = ticketId != null ? ticketId.toString() : "unknown";

            outboxService.save(
                    "Ticket",
                    aggregateId,
                    event.eventType(),
                    event.routingKey(),
                    event.data()
            );

            log.debug("Ticket event saved to outbox: type={}, aggregateId={}",
                    event.eventType(), aggregateId);

        } catch (Exception ex) {
            log.error("Failed to save ticket event to outbox: type={}", event.eventType(), ex);
        }
    }
}

package com.starterkit.ticket.ticket.infrastructure.config;

import com.starterkit.outbox.application.OutboxService;
import com.starterkit.ticket.ticket.application.event.TicketEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publishes TicketEvent via Transactional Outbox Pattern.
 *
 * Events are persisted to ticket.outbox_events within the SAME transaction
 * as the business entity. A separate scheduled OutboxPublisher then
 * forwards them to RabbitMQ.
 *
 * IMPORTANT: exceptions from {@link OutboxService#save} are intentionally
 * NOT caught here. If the outbox insert fails, the surrounding business
 * transaction must roll back — otherwise the business entity would be
 * committed while its event is silently lost, breaking atomicity.
 */
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
        Object ticketId = event.data().get("ticketId");
        String aggregateId = ticketId != null ? ticketId.toString() : "unknown";

        // Let exceptions propagate: they will roll back the caller's TX.
        // Never swallow here, or the event may be lost silently.
        outboxService.save(
                "Ticket",
                aggregateId,
                event.eventType(),
                event.routingKey(),
                event.data()
        );

        log.debug("Ticket event saved to outbox: type={}, aggregateId={}",
                event.eventType(), aggregateId);
    }
}

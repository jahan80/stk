package com.starterkit.auth.shared.infrastructure.event;

import com.starterkit.auth.auth.application.event.AuthEvent;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import com.starterkit.outbox.application.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publishes AuthEvent via Transactional Outbox Pattern.
 *
 * Events are persisted to auth.outbox_events within the SAME transaction
 * as the business entity. A separate scheduled OutboxPublisher then
 * forwards them to RabbitMQ.
 *
 * IMPORTANT: exceptions from {@link OutboxService#save} are intentionally
 * NOT caught here. If the outbox insert fails, the surrounding business
 * transaction must roll back — otherwise the business entity would be
 * committed while its event is silently lost, breaking atomicity.
 *
 * The default is "outbox". Set app.events.publisher=direct-rabbitmq only
 * in dev if you explicitly want the legacy direct mode.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.events.publisher",
        havingValue = "outbox",
        matchIfMissing = true
)
public class OutboxAuthEventPublisher implements AuthEventPublisher {

    private final OutboxService outboxService;

    @Override
    public void publish(AuthEvent event) {
        // FIX P1-5: derive a meaningful aggregateId from either
        // `userId` (business events) or `relatedUserId` (notif commands).
        Object userId = event.data().get("userId");
        if (userId == null) {
            userId = event.data().get("relatedUserId");
        }
        String aggregateId = userId != null ? userId.toString() : "unknown";

        // Let exceptions propagate: they will roll back the caller's TX.
        // Never swallow here, or the event may be lost silently.
        outboxService.save(
                "User",
                aggregateId,
                event.eventType(),
                event.routingKey(),
                event.data()
        );

        log.debug("Auth event saved to outbox: type={}, aggregateId={}",
                event.eventType(), aggregateId);
    }
}

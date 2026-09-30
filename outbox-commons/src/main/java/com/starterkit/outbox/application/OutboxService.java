package com.starterkit.outbox.application;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Save events to outbox table (same transaction as business entity).
 *
 * Usage:
 *   userRepo.save(user);
 *   outboxService.save("User", user.getId().toString(), "USER_REGISTERED",
 *                      "auth.user.registered", Map.of("userId", 123));
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final OutboxEventRepository repository;

    public OutboxEvent save(String aggregateType,
                             String aggregateId,
                             String eventType,
                             String routingKey,
                             Map<String, Object> payload) {

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setRoutingKey(routingKey);
        event.setPayload(payload);
        event.setTraceId(MDC.get(TRACE_ID_MDC_KEY));

        OutboxEvent saved = repository.save(event);

        log.debug("Outbox event saved: id={}, type={}, aggregate={}:{}",
                saved.getId(), eventType, aggregateType, aggregateId);

        return saved;
    }
}

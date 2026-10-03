package com.starterkit.audit.audit.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Observes messages routed to the audit-service DLQ.
 *
 * Only logs dead-lettered events for operator awareness. Messages stay
 * in the queue subject to its TTL / max-length configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditDlqListener {

    /** Matches the DLX routing key "audit.dead" configured in RabbitMqConfig. */
    @RabbitListener(queues = "audit.dlq")
    public void onDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        var headers = message.getMessageProperties().getHeaders();

        String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;

        log.error("AUDIT DEAD LETTER received: headers={}, body={}", headers, truncated);
    }
}

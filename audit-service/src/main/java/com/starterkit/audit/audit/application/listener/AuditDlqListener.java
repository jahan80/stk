package com.starterkit.audit.audit.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

/**
 * Observes messages routed to the audit-service DLQ.
 *
 * IMPORTANT: This listener is NOT registered as a @RabbitListener
 * by default, because RabbitMQ auto-ACKs successful listeners and
 * the message would be consumed (not preserved for investigation).
 *
 * DLQ messages are preserved subject to TTL / max-length.
 * Operators inspect via RabbitMQ management UI (port 15672).
 *
 * See NotifDlqListener for the full rationale.
 */
@Slf4j
@RequiredArgsConstructor
public class AuditDlqListener {

    public void onDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;
        log.error("AUDIT DEAD LETTER (observational): headers={}, body={}",
                message.getMessageProperties().getHeaders(), truncated);
    }
}

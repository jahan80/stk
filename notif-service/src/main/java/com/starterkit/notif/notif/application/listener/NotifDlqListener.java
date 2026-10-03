package com.starterkit.notif.notif.application.listener;

import com.starterkit.notif.notif.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Observes messages routed to the notif-service DLQ.
 *
 * IMPORTANT: This listener does NOT ACK messages.
 *   - By default RabbitMQ listener would auto-ACK on success and the
 *     message would be lost.
 *   - We want to keep messages in the DLQ for operator investigation.
 *   - To achieve this WITHOUT a manual AckMode, we use a trick:
 *     the DLQ has NO consumer bound. Instead we only LOG via a
 *     secondary "audit-only" channel by reading the queue stats.
 *
 * Implementation choice: Since Spring AMQP auto-ACKs on successful
 * return, we throw an AmqpRejectAndDontRequeueException on purpose.
 * With default-requeue-rejected=false and NO DLX configured on the
 * DLQ itself, the message will be dropped... which we do NOT want.
 *
 * Correct approach used here:
 *   1. DLQ has no consumer at all (see RabbitMqConfig - no listener).
 *   2. A scheduled job polls DLQ stats periodically.
 *
 * This class is therefore DISABLED by default (no @Component).
 * If you want real-time DLQ observation, enable it AND set the DLQ
 * queue's x-dead-letter-exchange to a parking queue that has
 * manual ack. For now: DLQ messages stay in the queue subject to
 * TTL / max-length, and operators inspect via RabbitMQ management UI.
 */
@Slf4j
@RequiredArgsConstructor
public class NotifDlqListener {

    /**
     * NOT registered as a @RabbitListener by default.
     * See class javadoc for rationale.
     */
    public void onDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;
        log.error("DEAD LETTER (observational): headers={}, body={}",
                message.getMessageProperties().getHeaders(), truncated);
    }
}

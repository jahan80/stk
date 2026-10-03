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
 * This listener does NOT process messages; it only logs them so that
 * operators can detect and investigate permanently-failed deliveries.
 * The message remains in the DLQ (until TTL or max-length kicks in).
 *
 * The DLQ has a TTL and max-length (see RabbitMqConfig), so it cannot
 * grow unbounded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifDlqListener {

    @RabbitListener(queues = RabbitMqConfig.DLQ_QUEUE)
    public void onDeadLetter(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        var headers = message.getMessageProperties().getHeaders();

        // Cap body log to avoid flooding the log with huge payloads.
        String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;

        log.error("DEAD LETTER received: headers={}, body={}", headers, truncated);
    }
}

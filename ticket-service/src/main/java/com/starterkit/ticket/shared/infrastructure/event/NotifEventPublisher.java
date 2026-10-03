package com.starterkit.ticket.shared.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Direct publisher for notif-service commands.
 *
 * These are NOT business events (those go through the outbox).
 * They are delivery commands — a missing command means a missing
 * email/SMS, but never a missing business transaction. If RabbitMQ is
 * briefly unavailable, the log captures it and the business TX is not
 * rolled back (notification delivery is best-effort from the
 * business-service side; notif-service owns retry).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventPublisher {

    public static final String EXCHANGE = "starterkit.events";

    private final RabbitTemplate rabbitTemplate;

    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.debug("Skipping email notification: no recipient");
            return;
        }

        try {
            String traceId = MDC.get("traceId");

            NotifEventMessage message = NotifEventMessage.sendEmail(
                    to, subject, body, traceId
            );

            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    "notif.email.send",
                    message,
                    m -> {
                        if (traceId != null) {
                            m.getMessageProperties().setHeader("X-Trace-Id", traceId);
                        }
                        return m;
                    }
            );

            log.info("Notif command sent: type=SEND_EMAIL, to={}, traceId={}",
                    to, traceId);

        } catch (Exception ex) {
            // Best-effort: do NOT break the business flow. The ticket
            // action succeeded; only the email is lost (acceptable for
            // ticket notifications in this starter kit).
            log.error("Failed to publish notif command: to={}", to, ex);
        }
    }
}

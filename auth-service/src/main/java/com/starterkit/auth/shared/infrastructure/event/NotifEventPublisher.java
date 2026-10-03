package com.starterkit.auth.shared.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventPublisher {

    public static final String EXCHANGE = "starterkit.events";

    private final RabbitTemplate rabbitTemplate;

    public void sendEmail(String to, String subject, String body) {
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

            log.info("Notif event published: type=SEND_EMAIL, to={}, traceId={}",
                    to, traceId);

        } catch (Exception ex) {
            log.error("Failed to publish notif event: to={}", to, ex);
        }
    }

    public void sendSms(String to, String message) {
        try {
            String traceId = MDC.get("traceId");

            NotifEventMessage msg = NotifEventMessage.sendSms(
                    to, message, traceId
            );

            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    "notif.sms.send",
                    msg,
                    m -> {
                        if (traceId != null) {
                            m.getMessageProperties().setHeader("X-Trace-Id", traceId);
                        }
                        return m;
                    }
            );

            log.info("Notif event published: type=SEND_SMS, to={}, traceId={}",
                    to, traceId);

        } catch (Exception ex) {
            log.error("Failed to publish notif event: to={}", to, ex);
        }
    }
}

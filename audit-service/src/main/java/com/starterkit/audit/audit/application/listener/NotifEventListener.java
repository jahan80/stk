package com.starterkit.audit.audit.application.listener;

import com.starterkit.audit.audit.api.dto.AuthEventMessage;
import com.starterkit.audit.audit.application.service.AuditEventService;
import com.starterkit.audit.audit.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifEventListener {

    private final AuditEventService auditEventService;

    @RabbitListener(queues = RabbitMqConfig.NOTIF_QUEUE)
    public void onNotifEvent(AuthEventMessage message) {
        log.debug("Received notif delivery event: type={}, eventId={}, traceId={}",
                message.getEventType(),
                message.getEventId(),
                message.getTraceId());
        try {
            auditEventService.saveFromMessage(message);
        } catch (Exception ex) {
            log.error("Failed to process notif delivery event: eventId={}, type={}",
                    message.getEventId(),
                    message.getEventType(),
                    ex);
            throw ex;
        }
    }
}

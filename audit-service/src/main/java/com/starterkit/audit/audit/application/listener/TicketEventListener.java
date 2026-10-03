package com.starterkit.audit.audit.application.listener;

import com.starterkit.audit.audit.api.dto.AuthEventMessage;
import com.starterkit.audit.audit.application.service.AuditEventService;
import com.starterkit.audit.audit.infrastructure.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes ticket-service business events and stores them as audit records.
 *
 * Message shape is identical to auth/gateway events (same envelope),
 * so we reuse AuthEventMessage as the DTO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private final AuditEventService auditEventService;

    @RabbitListener(queues = RabbitMqConfig.TICKET_QUEUE)
    public void onTicketEvent(AuthEventMessage message) {

        log.debug("Received ticket event: type={}, eventId={}, traceId={}",
                message.getEventType(),
                message.getEventId(),
                message.getTraceId());

        auditEventService.saveFromMessage(message);
    }
}

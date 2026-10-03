package com.starterkit.audit.audit.application.service;

import com.starterkit.audit.audit.api.dto.AuditEventResponse;
import com.starterkit.audit.audit.api.dto.AuthEventMessage;
import com.starterkit.audit.audit.domain.entity.AuditEvent;
import com.starterkit.audit.audit.domain.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public AuditEvent saveFromMessage(AuthEventMessage message) {

        // Validate eventId format. Malformed → reject to DLQ (no retry).
        UUID eventId;
        try {
            eventId = UUID.fromString(message.getEventId());
        } catch (Exception ex) {
            log.error("Invalid eventId format, rejecting: {}", message.getEventId(), ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid eventId: " + message.getEventId(), ex);
        }

        // Validate eventType. Missing → reject to DLQ.
        if (message.getEventType() == null || message.getEventType().isBlank()) {
            log.error("Missing eventType, rejecting: eventId={}", eventId);
            throw new AmqpRejectAndDontRequeueException(
                    "Missing eventType for eventId: " + eventId);
        }

        // Idempotency: duplicate → skip.
        if (auditEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate event received: eventId={} (skipping)", eventId);
            return null;
        }

        AuditEvent event = new AuditEvent();
        event.setEventId(eventId);
        event.setEventType(message.getEventType());
        event.setEventVersion(
                message.getEventVersion() != null
                        ? message.getEventVersion()
                        : "1.0"
        );
        event.setSource(message.getSource() != null
                ? message.getSource()
                : "unknown");
        event.setTraceId(message.getTraceId());
        // JSONB NOT NULL — fall back to empty map if payload is null
        event.setPayload(message.getData() != null ? message.getData() : java.util.Map.of());
        event.setOccurredAt(
                message.getOccurredAt() != null
                        ? message.getOccurredAt()
                        : Instant.now()
        );

        try {
            AuditEvent saved = auditEventRepository.save(event);

            log.info("Audit event saved: id={}, type={}, source={}, traceId={}",
                    saved.getId(),
                    saved.getEventType(),
                    saved.getSource(),
                    saved.getTraceId());

            return saved;

        } catch (DataIntegrityViolationException ex) {
            // Race condition: another consumer inserted the same event_id
            // between our existsByEventId check and this save.
            // The UNIQUE constraint on event_id caught it. Treat as duplicate.
            String msg = ex.getMessage();
            if (msg != null && msg.contains("audit_events_event_id_key")) {
                log.info("Duplicate audit event (race), skipping: eventId={}", eventId);
                return null;
            }
            throw ex;
        }
    }

    public Page<AuditEventResponse> search(
            String eventType,
            String source,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        // All filters are optional and AND-combined.
        // Normalize blank strings to null so predicates are skipped.
        String typeFilter = StringUtils.hasText(eventType) ? eventType : null;
        String sourceFilter = StringUtils.hasText(source) ? source : null;

        Page<AuditEvent> events = auditEventRepository.search(
                typeFilter, sourceFilter, from, to, pageable);

        return events.map(this::toResponse);
    }

    public List<AuditEventResponse> findByTraceId(String traceId) {
        return auditEventRepository.findByTraceId(traceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .eventVersion(event.getEventVersion())
                .source(event.getSource())
                .traceId(event.getTraceId())
                .payload(event.getPayload())
                .occurredAt(event.getOccurredAt())
                .receivedAt(event.getReceivedAt())
                .build();
    }
}

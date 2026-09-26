package com.starterkit.audit.audit.application.service;

import com.starterkit.audit.audit.api.dto.AuditEventResponse;
import com.starterkit.audit.audit.api.dto.AuthEventMessage;
import com.starterkit.audit.audit.domain.entity.AuditEvent;
import com.starterkit.audit.audit.domain.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

        UUID eventId = UUID.fromString(message.getEventId());

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
        event.setPayload(message.getData());
        event.setOccurredAt(
                message.getOccurredAt() != null
                        ? message.getOccurredAt()
                        : Instant.now()
        );

        AuditEvent saved = auditEventRepository.save(event);

        log.info("Audit event saved: id={}, type={}, source={}, traceId={}",
                saved.getId(),
                saved.getEventType(),
                saved.getSource(),
                saved.getTraceId());

        return saved;
    }

    public Page<AuditEventResponse> search(
            String eventType,
            String source,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Page<AuditEvent> events;

        boolean hasType = StringUtils.hasText(eventType);
        boolean hasSource = StringUtils.hasText(source);
        boolean hasRange = (from != null && to != null);

        if (hasType && hasSource) {
            events = auditEventRepository
                    .findByEventTypeAndSourceOrderByOccurredAtDesc(eventType, source, pageable);
        } else if (hasType) {
            events = auditEventRepository
                    .findByEventTypeOrderByOccurredAtDesc(eventType, pageable);
        } else if (hasSource) {
            events = auditEventRepository
                    .findBySourceOrderByOccurredAtDesc(source, pageable);
        } else if (hasRange) {
            events = auditEventRepository
                    .findByDateRange(from, to, pageable);
        } else {
            events = auditEventRepository
                    .findAllByOrderByOccurredAtDesc(pageable);
        }

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

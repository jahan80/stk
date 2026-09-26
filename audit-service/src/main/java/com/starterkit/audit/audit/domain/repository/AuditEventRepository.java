package com.starterkit.audit.audit.domain.repository;

import com.starterkit.audit.audit.domain.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTraceId(String traceId);

    boolean existsByEventId(UUID eventId);

    // Query 1: همه با page
    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    // Query 2: فقط eventType
    Page<AuditEvent> findByEventTypeOrderByOccurredAtDesc(String eventType, Pageable pageable);

    // Query 3: فقط source
    Page<AuditEvent> findBySourceOrderByOccurredAtDesc(String source, Pageable pageable);

    // Query 4: eventType + source
    Page<AuditEvent> findByEventTypeAndSourceOrderByOccurredAtDesc(
            String eventType, String source, Pageable pageable);

    // Query 5: با بازه زمانی
    @Query("SELECT a FROM AuditEvent a " +
           "WHERE a.occurredAt >= :from AND a.occurredAt <= :to " +
           "ORDER BY a.occurredAt DESC")
    Page<AuditEvent> findByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}

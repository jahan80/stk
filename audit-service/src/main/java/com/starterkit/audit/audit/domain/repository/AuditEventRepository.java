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

    /**
     * Combined-filter search.
     * All filters are optional and AND-combined.
     * Passing null for any filter disables that predicate.
     */
    @Query("""
        SELECT a FROM AuditEvent a
        WHERE (:eventType IS NULL OR a.eventType = :eventType)
          AND (:source IS NULL OR a.source = :source)
          AND (:from IS NULL OR a.occurredAt >= :from)
          AND (:to IS NULL OR a.occurredAt <= :to)
        ORDER BY a.occurredAt DESC
    """)
    Page<AuditEvent> search(
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}

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
     *
     * All filters are optional and AND-combined.
     * Passing null for any filter disables that predicate.
     *
     * NOTE: This is a native query with explicit casts because PostgreSQL
     * cannot infer parameter types inside `:param IS NULL` expressions.
     * Without the casts, PostgreSQL raises:
     *   "could not determine data type of parameter $N"
     */
    @Query(
            value = """
                SELECT * FROM audit.audit_events
                WHERE (CAST(:eventType AS TEXT) IS NULL OR event_type = CAST(:eventType AS TEXT))
                  AND (CAST(:source AS TEXT) IS NULL OR source = CAST(:source AS TEXT))
                  AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR occurred_at >= CAST(:from AS TIMESTAMPTZ))
                  AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR occurred_at <= CAST(:to AS TIMESTAMPTZ))
                ORDER BY occurred_at DESC
                """,
            countQuery = """
                SELECT COUNT(*) FROM audit.audit_events
                WHERE (CAST(:eventType AS TEXT) IS NULL OR event_type = CAST(:eventType AS TEXT))
                  AND (CAST(:source AS TEXT) IS NULL OR source = CAST(:source AS TEXT))
                  AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR occurred_at >= CAST(:from AS TIMESTAMPTZ))
                  AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR occurred_at <= CAST(:to AS TIMESTAMPTZ))
                """,
            nativeQuery = true
    )
    Page<AuditEvent> search(
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}

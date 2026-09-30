package com.starterkit.outbox.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transactional Outbox Event.
 *
 * Persisted in the same transaction as the business entity.
 * Published asynchronously by OutboxPublisher.
 *
 * Schema is set at runtime via Hibernate's default_schema.
 * Each service has its own table: auth.outbox_events, ticket.outbox_events, etc.
 */
@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_status_next_retry", columnList = "status,next_retry_at"),
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "routing_key", nullable = false, length = 200)
    private String routingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @PrePersist
    protected void onCreate() {
        if (eventId == null) eventId = UUID.randomUUID();
        createdAt = Instant.now();
        if (nextRetryAt == null) nextRetryAt = createdAt;
    }

    // ===== Helpers =====

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error != null && error.length() > 1000
                ? error.substring(0, 1000)
                : error;

        // At-least-once semantics: never give up.
        // Keep retrying with capped exponential backoff.
        // Truly unrecoverable events should be detected via
        // monitoring on `attempts` and handled operationally.
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = calculateNextRetry(this.attempts);
    }

    /**
     * Exponential backoff:
     *   attempt 1 → 5s
     *   attempt 2 → 30s
     *   attempt 3 → 2m
     *   attempt 4 → 10m
     *   attempt 5 → 1h
     */
    private Instant calculateNextRetry(int attempts) {
        // Exponential backoff with 1-hour cap.
        // attempt 1 -> 5s
        // attempt 2 -> 30s
        // attempt 3 -> 2m
        // attempt 4 -> 10m
        // attempt 5+ -> 1h (cap)
        long seconds = switch (Math.min(attempts, 5)) {
            case 1 -> 5;
            case 2 -> 30;
            case 3 -> 120;
            case 4 -> 600;
            default -> 3600;
        };
        return Instant.now().plusSeconds(seconds);
    }
}

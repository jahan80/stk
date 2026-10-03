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
 * Multi-instance safety is achieved via atomic claim:
 *   1. Publisher does UPDATE ... WHERE status=PENDING ... RETURNING *
 *      (with FOR UPDATE SKIP LOCKED inside subquery).
 *   2. Row becomes CLAIMED with claimed_by + locked_until.
 *   3. Publisher sends to RabbitMQ.
 *   4. On success → PUBLISHED. On failure → PENDING with backoff.
 *   5. If publisher dies before (4), reclaim job resets expired CLAIMED
 *      rows back to PENDING after locked_until passes.
 */
@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_status_next_retry", columnList = "status,next_retry_at"),
        @Index(name = "idx_outbox_created_at", columnList = "created_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id"),
        @Index(name = "idx_outbox_claim", columnList = "status,locked_until")
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

    // ===== Claim fields (multi-instance safety) =====

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @PrePersist
    protected void onCreate() {
        if (eventId == null) eventId = UUID.randomUUID();
        createdAt = Instant.now();
        if (nextRetryAt == null) nextRetryAt = createdAt;
    }

    // ===== State transitions =====

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
        this.claimedBy = null;
        this.claimedAt = null;
        this.lockedUntil = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error != null && error.length() > 1000
                ? error.substring(0, 1000)
                : error;

        // Back to PENDING for retry. Clear claim.
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = calculateNextRetry(this.attempts);
        this.claimedBy = null;
        this.claimedAt = null;
        this.lockedUntil = null;
    }

    /**
     * Exponential backoff with 1-hour cap.
     *   attempt 1 → 5s
     *   attempt 2 → 30s
     *   attempt 3 → 2m
     *   attempt 4 → 10m
     *   attempt 5+ → 1h
     */
    private Instant calculateNextRetry(int attempts) {
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

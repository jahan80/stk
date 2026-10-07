package com.starterkit.notif.notif.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

@Entity
@Table(name = "notifications", schema = "notif")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, unique = true)
    private UUID notificationId;

    /**
     * Source event UUID (from the producer service).
     * UNIQUE — one notification row per source event.
     * Retries reuse this same row (attempts++).
     */
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    // ===== Retry / attempts (V3) =====

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (nextAttemptAt == null) nextAttemptAt = now;
    }

    // ===== State transitions =====

    /** Provider call succeeded → terminal state. */
    public void markSent(String providerMessageId) {
        this.status = Status.SENT;
        this.providerMessageId = providerMessageId;
        this.sentAt = Instant.now();
        this.errorMessage = null;
        this.lastError = null;
        clearClaim();
    }

    /**
     * Provider call failed → keep row for retry.
     * @param rawError   technical message (provider exception / nack)
     * @param backoff    when to try again
     */
    public void markRetry(String rawError, Instant backoff) {
        this.status = Status.FAILED;
        this.attempts++;
        this.lastError = truncate(rawError, 2000);
        this.errorMessage = truncate(rawError, 500);
        this.nextAttemptAt = backoff;
        clearClaim();
    }

    public void claim(String instanceId, Instant leaseUntil) {
        this.claimedBy = instanceId;
        this.lockedUntil = leaseUntil;
    }

    public void clearClaim() {
        this.claimedBy = null;
        this.lockedUntil = null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public enum Channel { SMS, EMAIL, PUSH }

    public enum Status {
        PENDING,   // never attempted yet
        SENT,      // terminal success
        FAILED     // attempted, will be retried until maxAttempts
    }
}

package com.starterkit.notif.notif.domain.repository;

import com.starterkit.notif.notif.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(UUID notificationId);

    boolean existsByNotificationId(UUID notificationId);

    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    @Query("""
        SELECT n FROM Notification n
        WHERE (:channel IS NULL OR n.channel = :channel)
          AND (:status  IS NULL OR n.status  = :status)
          AND (:recipient IS NULL
               OR LOWER(n.recipient) LIKE LOWER(CONCAT('%', :recipient, '%')))
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> search(
            @Param("channel") Notification.Channel channel,
            @Param("status") Notification.Status status,
            @Param("recipient") String recipient,
            Pageable pageable
    );

    // =====================================================
    // Retry job queries (V3)
    // =====================================================

    /**
     * Atomically claim a batch of retry-eligible notifications.
     *
     * Eligible = status IN (PENDING, FAILED)
     *            AND next_attempt_at <= NOW()
     *            AND (locked_until IS NULL OR locked_until < NOW())
     *            AND attempts < :maxAttempts
     *
     * Uses FOR UPDATE SKIP LOCKED for multi-instance safety.
     */
    @Query(value = """
        UPDATE notif.notifications
        SET claimed_by = :instanceId,
            locked_until = NOW() + (:leaseSeconds * INTERVAL '1 second')
        WHERE id IN (
            SELECT id FROM notif.notifications
            WHERE status IN ('PENDING', 'FAILED')
              AND next_attempt_at <= NOW()
              AND (locked_until IS NULL OR locked_until < NOW())
              AND attempts < :maxAttempts
            ORDER BY next_attempt_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    List<Notification> claimRetryBatch(
            @Param("instanceId") String instanceId,
            @Param("batchSize") int batchSize,
            @Param("leaseSeconds") int leaseSeconds,
            @Param("maxAttempts") int maxAttempts);

    /**
     * Reset expired claims back to retry-eligible.
     * Only clears the lease; does NOT reset attempts.
     */
    @Modifying
    @Query(value = """
        UPDATE notif.notifications
        SET claimed_by = NULL,
            locked_until = NULL
        WHERE status IN ('PENDING', 'FAILED')
          AND locked_until IS NOT NULL
          AND locked_until < NOW()
        """, nativeQuery = true)
    int reclaimExpired();

    /**
     * Give-up query: rows that exhausted attempts and are still FAILED.
     * Used for observability / alerts.
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.status = 'FAILED' AND n.attempts >= :maxAttempts
    """)
    long countExhausted(@Param("maxAttempts") int maxAttempts);
}

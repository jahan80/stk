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

    /**
     * Combined-filter search.
     *
     * The upper-level service still passes enums for channel/status
     * (which Hibernate serializes as strings because the entity uses
     * @Enumerated(EnumType.STRING)). The native query uses explicit
     * TEXT casts to avoid PostgreSQL's "function lower(bytea) does
     * not exist" error when :recipient is null.
     */
    @Query(value = """
        SELECT * FROM notif.notifications n
        WHERE (CAST(:channel AS TEXT) IS NULL OR n.channel = CAST(:channel AS TEXT))
          AND (CAST(:status  AS TEXT) IS NULL OR n.status  = CAST(:status  AS TEXT))
          AND (CAST(:recipient AS TEXT) IS NULL
               OR LOWER(n.recipient) LIKE LOWER(CONCAT('%', CAST(:recipient AS TEXT), '%')))
        ORDER BY n.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM notif.notifications n
        WHERE (CAST(:channel AS TEXT) IS NULL OR n.channel = CAST(:channel AS TEXT))
          AND (CAST(:status  AS TEXT) IS NULL OR n.status  = CAST(:status  AS TEXT))
          AND (CAST(:recipient AS TEXT) IS NULL
               OR LOWER(n.recipient) LIKE LOWER(CONCAT('%', CAST(:recipient AS TEXT), '%')))
        """,
        nativeQuery = true)
    Page<Notification> search(
            @Param("channel") String channel,
            @Param("status") String status,
            @Param("recipient") String recipient,
            Pageable pageable);

    // =====================================================
    // Retry job queries (V3)
    // =====================================================

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

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.status = 'FAILED' AND n.attempts >= :maxAttempts
    """)
    long countExhausted(@Param("maxAttempts") int maxAttempts);

    // =====================================================
    // IN_APP queries (step D)
    // =====================================================

    @Query("""
        SELECT n FROM Notification n
        WHERE n.channel = 'IN_APP'
          AND n.recipientUserId = :userId
          AND (:unreadOnly = false OR n.readAt IS NULL)
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findInAppForUser(
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.channel = 'IN_APP'
          AND n.recipientUserId = :userId
          AND n.readAt IS NULL
    """)
    long countUnreadInApp(@Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.readAt = :now
        WHERE n.id = :id AND n.recipientUserId = :userId AND n.readAt IS NULL
    """)
    int markRead(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.readAt = :now
        WHERE n.channel = 'IN_APP' AND n.recipientUserId = :userId AND n.readAt IS NULL
    """)
    int markAllRead(@Param("userId") Long userId,
                    @Param("now") Instant now);
}

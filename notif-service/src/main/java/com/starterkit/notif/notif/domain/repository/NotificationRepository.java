package com.starterkit.notif.notif.domain.repository;

import com.starterkit.notif.notif.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(UUID notificationId);

    boolean existsByNotificationId(UUID notificationId);

    /**
     * Idempotency guard: check if an event has already been turned
     * into a notification. Used by the consumer to skip duplicate
     * RabbitMQ deliveries.
     */
    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    /**
     * Combined-filter search.
     *
     * All three filters are optional and AND-combined:
     *   - channel   : exact match (enum name)
     *   - status    : exact match (enum name)
     *   - recipient : partial match (case-insensitive LIKE)
     *
     * Passing null for any filter disables that predicate.
     */
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
}

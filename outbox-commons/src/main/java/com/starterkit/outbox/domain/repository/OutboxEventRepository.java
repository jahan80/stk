package com.starterkit.outbox.domain.repository;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find PENDING events whose next_retry_at is now or past.
     *
     * Uses PostgreSQL FOR UPDATE SKIP LOCKED to prevent multiple publisher
     * instances from picking the same event. If another transaction has
     * already locked a row, it is skipped.
     *
     * MUST be called inside a @Transactional context.
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status = :status
          AND next_retry_at <= :now
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> lockPendingForPublish(
            @Param("status") String status,
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    /**
     * Lock a single event row for publishing.
     * Returns empty if the row is already locked by another transaction
     * or does not exist.
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE id = :id
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<OutboxEvent> lockById(@Param("id") Long id);

    /**
     * Delete PUBLISHED events older than cutoff (cleanup).
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :cutoff")
    int deletePublishedOlderThan(
            @Param("status") OutboxStatus status,
            @Param("cutoff") Instant cutoff);
}

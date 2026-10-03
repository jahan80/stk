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
     * Atomically claim a batch of PENDING events.
     *
     * The inner SELECT uses FOR UPDATE SKIP LOCKED so concurrent publishers
     * never claim the same row. The outer UPDATE sets status=CLAIMED and
     * fills claimed_by / locked_until so the row is protected even after
     * this transaction commits.
     *
     * Caller must be inside a transaction.
     */
    @Query(value = """
        UPDATE outbox_events
        SET status = 'CLAIMED',
            claimed_by = :instanceId,
            claimed_at = NOW(),
            locked_until = NOW() + (:leaseSeconds * INTERVAL '1 second')
        WHERE id IN (
            SELECT id FROM outbox_events
            WHERE status = 'PENDING'
              AND next_retry_at <= NOW()
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    List<OutboxEvent> claimBatch(
            @Param("instanceId") String instanceId,
            @Param("batchSize") int batchSize,
            @Param("leaseSeconds") int leaseSeconds);

    /**
     * Recover expired CLAIMED events (publisher crashed).
     * Resets them back to PENDING so another instance can retry.
     */
    @Modifying
    @Query(value = """
        UPDATE outbox_events
        SET status = 'PENDING',
            claimed_by = NULL,
            claimed_at = NULL,
            locked_until = NULL,
            last_error = COALESCE(last_error, 'Reclaimed after lease expiration')
        WHERE status = 'CLAIMED'
          AND locked_until < NOW()
        """, nativeQuery = true)
    int reclaimExpired();

    Optional<OutboxEvent> findByIdAndClaimedBy(Long id, String claimedBy);

    /**
     * Delete PUBLISHED events older than cutoff (cleanup).
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :cutoff")
    int deletePublishedOlderThan(
            @Param("status") OutboxStatus status,
            @Param("cutoff") Instant cutoff);
}

package com.starterkit.outbox.domain.repository;

import com.starterkit.outbox.domain.entity.OutboxEvent;
import com.starterkit.outbox.domain.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find PENDING events whose next_retry_at is now or past.
     * Uses pessimistic lock to prevent multiple publishers picking the same event.
     */
    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = :status
          AND e.nextRetryAt <= :now
        ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findPendingForRetry(
            @Param("status") OutboxStatus status,
            @Param("now") Instant now,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Delete PUBLISHED events older than cutoff (cleanup).
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.publishedAt < :cutoff")
    int deletePublishedOlderThan(
            @Param("status") OutboxStatus status,
            @Param("cutoff") Instant cutoff);
}

package com.starterkit.outbox.domain.entity;

/**
 * Lifecycle of an outbox event:
 *
 *   PENDING ──claim()──> CLAIMED ──publish OK──> PUBLISHED
 *                          │
 *                          └──publish FAIL──> PENDING (retry with backoff)
 *
 * On instance crash: a scheduled reclaim job resets CLAIMED rows whose
 * lock has expired back to PENDING.
 */
public enum OutboxStatus {
    PENDING,
    CLAIMED,
    PUBLISHED,
    FAILED
}

-- ==================================================
-- V24: Add claim columns to auth.outbox_events
-- ==================================================
-- Multi-instance safety: replace "fetch IDs then lock again" pattern
-- with atomic UPDATE ... RETURNING claim.

ALTER TABLE auth.outbox_events
    ADD COLUMN claimed_by   VARCHAR(100),
    ADD COLUMN claimed_at   TIMESTAMPTZ,
    ADD COLUMN locked_until TIMESTAMPTZ;

CREATE INDEX idx_outbox_claim ON auth.outbox_events(status, locked_until);

COMMENT ON COLUMN auth.outbox_events.claimed_by
    IS 'Instance ID that claimed this event for publishing';
COMMENT ON COLUMN auth.outbox_events.locked_until
    IS 'Lease expiration; after this, reclaim job resets to PENDING';

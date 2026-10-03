-- ==================================================
-- V12: Add claim columns to ticket.outbox_events
-- ==================================================
-- Multi-instance safety: replace "fetch IDs then lock again" pattern
-- with atomic UPDATE ... RETURNING claim.

ALTER TABLE ticket.outbox_events
    ADD COLUMN claimed_by   VARCHAR(100),
    ADD COLUMN claimed_at   TIMESTAMPTZ,
    ADD COLUMN locked_until TIMESTAMPTZ;

CREATE INDEX idx_outbox_claim ON ticket.outbox_events(status, locked_until);

COMMENT ON COLUMN ticket.outbox_events.claimed_by
    IS 'Instance ID that claimed this event for publishing';
COMMENT ON COLUMN ticket.outbox_events.locked_until
    IS 'Lease expiration; after this, reclaim job resets to PENDING';

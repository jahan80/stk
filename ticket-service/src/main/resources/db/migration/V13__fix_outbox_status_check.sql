-- ==================================================
-- V13: Add CLAIMED to outbox_events status check
-- ==================================================
-- V12 introduced CLAIMED status for atomic claim,
-- but the original CHECK constraint (V10) only allowed
-- PENDING, PUBLISHED, FAILED. This migration aligns them.

ALTER TABLE ticket.outbox_events
    DROP CONSTRAINT IF EXISTS chk_outbox_status;

ALTER TABLE ticket.outbox_events
    ADD CONSTRAINT chk_outbox_status
    CHECK (status IN ('PENDING', 'CLAIMED', 'PUBLISHED', 'FAILED'));

COMMENT ON CONSTRAINT chk_outbox_status ON ticket.outbox_events
    IS 'Outbox event lifecycle: PENDING, CLAIMED, PUBLISHED, FAILED';

-- ==================================================
-- V25: Add CLAIMED to outbox_events status check
-- ==================================================
-- V24 introduced CLAIMED status for atomic claim,
-- but the original CHECK constraint (V23) only allowed
-- PENDING, PUBLISHED, FAILED. This migration aligns them.

ALTER TABLE auth.outbox_events
    DROP CONSTRAINT IF EXISTS chk_outbox_status;

ALTER TABLE auth.outbox_events
    ADD CONSTRAINT chk_outbox_status
    CHECK (status IN ('PENDING', 'CLAIMED', 'PUBLISHED', 'FAILED'));

COMMENT ON CONSTRAINT chk_outbox_status ON auth.outbox_events
    IS 'Outbox event lifecycle: PENDING, CLAIMED, PUBLISHED, FAILED';

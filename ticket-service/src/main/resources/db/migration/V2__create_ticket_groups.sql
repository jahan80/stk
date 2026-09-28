-- ==================================================
-- V2: Ticket groups (support teams)
-- ==================================================

CREATE TABLE ticket.ticket_groups (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_groups_enabled ON ticket.ticket_groups(enabled);

COMMENT ON TABLE ticket.ticket_groups IS 'Support teams/groups';

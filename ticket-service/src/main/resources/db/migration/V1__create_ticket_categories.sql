-- ==================================================
-- V1: Ticket categories (ITIL-based)
-- ==================================================

CREATE TABLE ticket.ticket_categories (
    id              BIGSERIAL    PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_code    ON ticket.ticket_categories(code);
CREATE INDEX idx_categories_enabled ON ticket.ticket_categories(enabled);

COMMENT ON TABLE ticket.ticket_categories IS 'ITIL-based ticket categories';

-- Seed (ITIL standard)
INSERT INTO ticket.ticket_categories (code, name, description, enabled, display_order) VALUES
    ('IR', 'Incident Request', 'Report an incident or unexpected behavior', TRUE, 1),
    ('CR', 'Change Request',   'Request a change or new feature',           TRUE, 2),
    ('SR', 'Service Request',  'Request a service or ask a question',       TRUE, 3);

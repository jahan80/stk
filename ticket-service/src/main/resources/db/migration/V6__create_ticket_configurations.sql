-- ==================================================
-- V6: Ticket configurations
-- ==================================================

CREATE TABLE ticket.configurations (
    id              BIGSERIAL    PRIMARY KEY,
    config_key      VARCHAR(200) NOT NULL UNIQUE,
    config_value    TEXT         NOT NULL,
    default_value   TEXT         NOT NULL,
    value_type      VARCHAR(20)  NOT NULL,
    description     VARCHAR(500),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_configs_key ON ticket.configurations(config_key);

COMMENT ON TABLE ticket.configurations IS 'Ticket service configurations';

-- ==================================================
-- Seed
-- ==================================================
INSERT INTO ticket.configurations 
    (config_key, config_value, default_value, value_type, description, enabled)
VALUES
    -- Ticket number
    ('TICKET.NUMBER.PREFIX', 'STK', 'STK', 'STRING',
     'Ticket number prefix (e.g. STK-0001)', TRUE),

    ('TICKET.NUMBER.PADDING', '4', '4', 'INTEGER',
     'Zero padding for ticket number', TRUE),

    -- Feature toggles
    ('TICKET.CREATE.ENABLED', 'true', 'true', 'BOOLEAN',
     'Allow creating new tickets', TRUE),

    ('TICKET.ASSIGN.ENABLED', 'true', 'true', 'BOOLEAN',
     'Allow assigning tickets to users/groups', TRUE),

    ('TICKET.COMMENT.ENABLED', 'true', 'true', 'BOOLEAN',
     'Allow comments on tickets', TRUE),

    ('TICKET.COMMENT.MAX_LENGTH', '5000', '5000', 'INTEGER',
     'Maximum comment length', TRUE),

    -- SLA (hours)
    ('TICKET.SLA.URGENT.HOURS', '2', '2', 'INTEGER',
     'SLA in hours for URGENT priority', TRUE),

    ('TICKET.SLA.HIGH.HOURS', '8', '8', 'INTEGER',
     'SLA in hours for HIGH priority', TRUE),

    ('TICKET.SLA.MEDIUM.HOURS', '24', '24', 'INTEGER',
     'SLA in hours for MEDIUM priority', TRUE),

    ('TICKET.SLA.LOW.HOURS', '72', '72', 'INTEGER',
     'SLA in hours for LOW priority', TRUE),

    -- Auto-close
    ('TICKET.AUTO_CLOSE.ENABLED', 'false', 'false', 'BOOLEAN',
     'Auto-close RESOLVED tickets after N days', TRUE),

    ('TICKET.AUTO_CLOSE.RESOLVED.DAYS', '7', '7', 'INTEGER',
     'Days in RESOLVED before auto-close', TRUE),

    -- Notifications
    ('TICKET.NOTIFY.ADMIN.ON_CREATE', 'true', 'true', 'BOOLEAN',
     'Notify admins when ticket is created', TRUE),

    ('TICKET.NOTIFY.OWNER.ON_COMMENT', 'true', 'true', 'BOOLEAN',
     'Notify owner when someone comments', TRUE);

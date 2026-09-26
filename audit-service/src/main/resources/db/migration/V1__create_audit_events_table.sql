-- ==================================================
-- V1: Audit events table
-- ==================================================
CREATE TABLE audit.audit_events (
    id              BIGSERIAL    PRIMARY KEY,
    event_id        UUID         NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    event_version   VARCHAR(20)  NOT NULL DEFAULT '1.0',
    source          VARCHAR(50)  NOT NULL,
    trace_id        VARCHAR(100),
    payload         JSONB        NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    received_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_type ON audit.audit_events(event_type);
CREATE INDEX idx_audit_events_trace_id ON audit.audit_events(trace_id);
CREATE INDEX idx_audit_events_source ON audit.audit_events(source);
CREATE INDEX idx_audit_events_occurred_at ON audit.audit_events(occurred_at DESC);

COMMENT ON TABLE audit.audit_events IS 'Stores audit events from all services';
COMMENT ON COLUMN audit.audit_events.event_id IS 'Unique event identifier';
COMMENT ON COLUMN audit.audit_events.event_type IS 'e.g. USER_REGISTERED, USER_LOGGED_IN';
COMMENT ON COLUMN audit.audit_events.source IS 'Service that emitted the event (e.g. auth-service)';
COMMENT ON COLUMN audit.audit_events.payload IS 'Full event payload (JSON)';

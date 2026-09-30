CREATE TABLE ticket.outbox_events (
    id              BIGSERIAL       PRIMARY KEY,
    event_id        UUID            NOT NULL UNIQUE,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(100)    NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    routing_key     VARCHAR(200)    NOT NULL,
    payload         JSONB           NOT NULL,
    trace_id        VARCHAR(100),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempts        INT             NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    next_retry_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_status_next_retry ON ticket.outbox_events (status, next_retry_at);
CREATE INDEX idx_outbox_created_at       ON ticket.outbox_events (created_at);
CREATE INDEX idx_outbox_aggregate        ON ticket.outbox_events (aggregate_type, aggregate_id);

-- ==================================================
-- V1: Rate Limits table
-- ==================================================
CREATE TABLE gateway.rate_limits (
    id                          BIGSERIAL    PRIMARY KEY,
    path_pattern                VARCHAR(200) NOT NULL,
    method                      VARCHAR(10),
    key_type                    VARCHAR(20)  NOT NULL DEFAULT 'IP_PATH',
    requests_per_window         INT          NOT NULL,
    window_seconds              INT          NOT NULL,
    burst_capacity              INT,
    default_requests_per_window INT          NOT NULL,
    default_window_seconds      INT          NOT NULL,
    default_burst_capacity      INT,
    default_enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled                     BOOLEAN      NOT NULL DEFAULT TRUE,
    priority                    INT          NOT NULL DEFAULT 0,
    description                 VARCHAR(255),
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_rate_limits_path_method_key 
        UNIQUE (path_pattern, method, key_type)
);

CREATE INDEX idx_rate_limits_enabled ON gateway.rate_limits(enabled);
CREATE INDEX idx_rate_limits_priority ON gateway.rate_limits(priority DESC);
CREATE INDEX idx_rate_limits_path ON gateway.rate_limits(path_pattern);

COMMENT ON TABLE gateway.rate_limits IS 'Rate limit configurations';
COMMENT ON COLUMN gateway.rate_limits.key_type IS 'IP, IP_PATH, USER, USER_PATH';
COMMENT ON COLUMN gateway.rate_limits.priority IS 'Higher priority matches first';

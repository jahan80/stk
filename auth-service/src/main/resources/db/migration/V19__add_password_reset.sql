CREATE TABLE auth.password_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMPTZ,
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id 
    ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at 
    ON auth.password_reset_tokens(expires_at);

COMMENT ON TABLE auth.password_reset_tokens 
    IS 'Password reset tokens (hashed)';

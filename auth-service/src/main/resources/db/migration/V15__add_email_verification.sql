ALTER TABLE auth.users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_users_email_verified ON auth.users(email_verified);

CREATE TABLE auth.email_verification_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user_id 
    ON auth.email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_expires_at 
    ON auth.email_verification_tokens(expires_at);

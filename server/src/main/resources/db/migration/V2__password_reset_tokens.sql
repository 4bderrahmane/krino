CREATE TABLE password_reset_tokens (
    id UUID NOT NULL,
    token_hash BYTEA NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_password_reset_tokens_token_hash_length CHECK (octet_length(token_hash) = 32),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);
-- Only unused tokens are ever looked up; keep that lookup lean.
CREATE INDEX idx_password_reset_tokens_active_user ON password_reset_tokens (user_id)
    WHERE used = FALSE;

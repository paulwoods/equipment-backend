CREATE TABLE password_reset_token
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    token      VARCHAR(512) NOT NULL UNIQUE,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_token_token ON password_reset_token (token);

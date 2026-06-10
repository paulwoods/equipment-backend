-- Tokens are now stored as SHA-256 hashes (hex) instead of plaintext.
-- Existing plaintext rows cannot be migrated, so invalidate them all;
-- users simply log in again / request a new password reset.
DELETE FROM refresh_token;
DELETE FROM password_reset_token;

-- SHA-256 hex is always exactly 64 characters; narrow the columns so the
-- schema rejects anything that is not a hash (e.g. an accidental plaintext write).
ALTER TABLE refresh_token ALTER COLUMN token TYPE VARCHAR(64);
ALTER TABLE password_reset_token ALTER COLUMN token TYPE VARCHAR(64);

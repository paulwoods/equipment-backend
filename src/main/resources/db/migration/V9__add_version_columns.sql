-- Optimistic locking: @Version columns so concurrent edits fail with 409
-- instead of silently overwriting each other.
ALTER TABLE equipment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE procedure ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

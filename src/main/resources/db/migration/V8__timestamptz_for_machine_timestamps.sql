-- Machine timestamps (token expiry, account creation) must be timezone-aware.
-- Plain TIMESTAMP stores wall-clock time and shifts meaning with the JVM/DB
-- timezone; TIMESTAMPTZ stores an absolute instant. Existing values are
-- interpreted in the database session timezone, matching how the application
-- previously wrote them via LocalDateTime.now().
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE refresh_token ALTER COLUMN expires_at TYPE TIMESTAMPTZ;
ALTER TABLE password_reset_token ALTER COLUMN expires_at TYPE TIMESTAMPTZ;
ALTER TABLE password_reset_token ALTER COLUMN created_at TYPE TIMESTAMPTZ;

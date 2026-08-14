-- Google sign-in. Users provisioned through Google have no local password, so
-- the column becomes nullable; google_sub holds Google's stable subject
-- identifier, which (unlike email) never changes for an account.
ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN google_sub VARCHAR(255);

ALTER TABLE users
    ADD CONSTRAINT users_google_sub_key UNIQUE (google_sub);

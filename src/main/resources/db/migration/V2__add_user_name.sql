ALTER TABLE users
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '';
UPDATE users
SET name = email
WHERE name = '';

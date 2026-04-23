ALTER TABLE users
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT 'x';
UPDATE users
SET name = email
WHERE name = '';

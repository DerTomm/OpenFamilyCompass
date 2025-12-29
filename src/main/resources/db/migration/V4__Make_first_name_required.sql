-- Make first_name required for all users

-- Set default first_name to username for users that don't have it set
UPDATE users SET first_name = username WHERE first_name IS NULL OR first_name = '';

-- Make first_name column not null
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
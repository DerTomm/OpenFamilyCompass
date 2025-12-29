-- Add language preference column to users table
ALTER TABLE users ADD COLUMN language VARCHAR(2);

-- Add comment to the column
COMMENT ON COLUMN users.language IS 'User preferred language: en, de, or null for browser default';
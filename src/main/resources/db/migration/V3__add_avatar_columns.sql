-- Add avatar columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_type VARCHAR(20) DEFAULT 'DEFAULT';
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_icon_name VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_data BYTEA;

-- Add comment for documentation
COMMENT ON COLUMN users.avatar_type IS 'Avatar type: DEFAULT, ICON, or PHOTO';
COMMENT ON COLUMN users.avatar_icon_name IS 'Name of selected icon avatar (e.g., lion, panda)';
COMMENT ON COLUMN users.avatar_data IS 'Binary data for uploaded photo avatar';

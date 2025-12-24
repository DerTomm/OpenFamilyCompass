-- Migration: Add theme column to users table
-- Date: 2025-12-24
-- Description: Adds user theme preference (LIGHT or DARK mode) to users table

ALTER TABLE users ADD COLUMN IF NOT EXISTS theme VARCHAR(10) DEFAULT 'LIGHT';

-- Update existing users to have LIGHT theme as default
UPDATE users SET theme = 'LIGHT' WHERE theme IS NULL;

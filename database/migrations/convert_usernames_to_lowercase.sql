-- Migration: Convert all usernames to lowercase
-- Date: 2025-12-24
-- Description: Makes username case-insensitive by converting all existing usernames to lowercase

UPDATE users SET username = LOWER(username);

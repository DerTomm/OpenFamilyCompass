-- Add total_points and first_name to users table
ALTER TABLE users ADD COLUMN total_points int4 NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN first_name varchar(255);
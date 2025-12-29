-- Migrate from children table to users table

-- First, migrate data from children to users
UPDATE users
SET total_points = c.total_points,
    first_name = c.first_name
FROM children c
WHERE users.id = c.user_id;

-- Rename columns in point_transactions
ALTER TABLE point_transactions RENAME COLUMN child_id TO user_id;

-- Rename columns in reward_redemptions
ALTER TABLE reward_redemptions RENAME COLUMN child_id TO user_id;

-- Rename columns in tasks
ALTER TABLE tasks RENAME COLUMN assigned_child_id TO assigned_user_id;

-- Rename columns in behaviors
ALTER TABLE behaviors RENAME COLUMN child_id TO user_id;

-- Rename columns in behavior_evaluations
ALTER TABLE behavior_evaluations RENAME COLUMN child_id TO user_id;

-- Rename columns in penalties
ALTER TABLE penalties RENAME COLUMN child_id TO user_id;

-- Drop foreign key constraints that reference children table
ALTER TABLE point_transactions DROP CONSTRAINT fk_children_child_id;
ALTER TABLE reward_redemptions DROP CONSTRAINT fk_children_child_id;
ALTER TABLE tasks DROP CONSTRAINT fk_children_assigned_child_id;
ALTER TABLE behaviors DROP CONSTRAINT fk_children_child_id;
ALTER TABLE behavior_evaluations DROP CONSTRAINT fk_children_child_id;
ALTER TABLE penalties DROP CONSTRAINT fk_childen_child_id;

-- Add new foreign key constraints to users table
ALTER TABLE point_transactions ADD CONSTRAINT fk_users_user_id FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE reward_redemptions ADD CONSTRAINT fk_users_user_id FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE tasks ADD CONSTRAINT fk_users_assigned_user_id FOREIGN KEY (assigned_user_id) REFERENCES users(id);
ALTER TABLE behaviors ADD CONSTRAINT fk_users_user_id FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE behavior_evaluations ADD CONSTRAINT fk_users_user_id FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE penalties ADD CONSTRAINT fk_users_user_id FOREIGN KEY (user_id) REFERENCES users(id);

-- Drop the children table
DROP TABLE children;
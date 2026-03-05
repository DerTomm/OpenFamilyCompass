-- Add optional user restriction to rewards
-- When user_id is NULL the reward is visible to all children
ALTER TABLE rewards ADD COLUMN user_id BIGINT;
ALTER TABLE rewards ADD CONSTRAINT fk_rewards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

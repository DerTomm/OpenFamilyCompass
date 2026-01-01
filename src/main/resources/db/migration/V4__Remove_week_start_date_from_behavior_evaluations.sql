-- Remove week_start_date column from behavior_evaluations table
-- The evaluation period is now determined by the committed flag, not by date
ALTER TABLE behavior_evaluations DROP COLUMN week_start_date;

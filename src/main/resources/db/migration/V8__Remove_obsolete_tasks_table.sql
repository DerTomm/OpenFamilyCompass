-- V8: Remove obsolete tasks table and related code

-- Remove the old tasks table as it's no longer used
-- All task functionality has been migrated to task_definitions and task_instances
DROP TABLE IF EXISTS tasks;
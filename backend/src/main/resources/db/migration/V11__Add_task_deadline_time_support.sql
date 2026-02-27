ALTER TABLE task_definitions
    ADD COLUMN IF NOT EXISTS end_at timestamp;

ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS due_at timestamp;

UPDATE task_definitions
SET end_at = end_date::timestamp
WHERE end_at IS NULL
  AND end_date IS NOT NULL;

UPDATE task_instances
SET due_at = due_date::timestamp
WHERE due_at IS NULL
  AND due_date IS NOT NULL;

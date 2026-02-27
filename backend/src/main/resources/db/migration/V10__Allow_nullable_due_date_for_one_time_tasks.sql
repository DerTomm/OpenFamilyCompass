-- Allow tasks without a deadline (optional due date for ONCE tasks)
ALTER TABLE task_instances
    ALTER COLUMN due_date DROP NOT NULL;

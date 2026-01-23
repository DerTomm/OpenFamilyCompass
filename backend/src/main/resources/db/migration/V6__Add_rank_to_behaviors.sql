-- Add rank column to behaviors table for ordering
ALTER TABLE behaviors ADD COLUMN rank INTEGER NOT NULL DEFAULT 0;

-- Set initial ranks based on creation order
UPDATE behaviors SET rank = sub.row_num
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) - 1 AS row_num
    FROM behaviors
) sub
WHERE behaviors.id = sub.id;
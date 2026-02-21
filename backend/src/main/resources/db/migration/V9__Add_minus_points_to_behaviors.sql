-- V9: Add minus_points column to behaviors table for new evaluation logic
-- Behavior evaluations now support both plus and minus points (range: -minusPoints .. +plusPoints)

-- Add minus_points column (stored as positive number, e.g., 5 means -5..0 range)
ALTER TABLE behaviors ADD COLUMN minus_points INTEGER NOT NULL DEFAULT 0;

-- Existing behaviors default to minusPoints=0 (no negative range)
-- New evaluations will start at 0 instead of max points

-- Migration: Rename habits table to behaviors and add guideline field
-- This migration renames the habits table to behaviors and changes the description field to guideline

-- Rename the table
ALTER TABLE habits RENAME TO behaviors;

-- Rename the description column to guideline and make it NOT NULL
ALTER TABLE behaviors RENAME COLUMN description TO guideline;
ALTER TABLE behaviors ALTER COLUMN guideline SET NOT NULL;

-- Update any existing point transactions to use the new type
UPDATE point_transactions 
SET type = 'BEHAVIOR' 
WHERE type = 'HABIT';

-- Update the sequence if it exists
ALTER SEQUENCE IF EXISTS habits_id_seq RENAME TO behaviors_id_seq;

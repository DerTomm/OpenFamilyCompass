-- Add image storage fields to rewards table
ALTER TABLE rewards ADD COLUMN image_data BYTEA;
ALTER TABLE rewards ADD COLUMN image_content_type VARCHAR(50);

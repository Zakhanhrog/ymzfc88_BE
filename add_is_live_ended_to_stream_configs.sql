-- Migration: Add is_live_ended column to stream_configs table
-- Date: 2024

ALTER TABLE stream_configs 
ADD COLUMN is_live_ended BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing records to have is_live_ended = false
UPDATE stream_configs SET is_live_ended = FALSE WHERE is_live_ended IS NULL;


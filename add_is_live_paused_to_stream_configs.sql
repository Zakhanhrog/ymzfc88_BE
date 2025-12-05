-- Migration: Add is_live_paused column to stream_configs table
-- Date: 2024

ALTER TABLE stream_configs 
ADD COLUMN is_live_paused BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing records to have is_live_paused = false
UPDATE stream_configs SET is_live_paused = FALSE WHERE is_live_paused IS NULL;


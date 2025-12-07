-- Migration: Add details column to promotions table
-- Run this SQL script to add the details column for rich text content

ALTER TABLE promotions 
ADD COLUMN details TEXT NULL AFTER description;

-- Update existing records if needed (optional)
-- UPDATE promotions SET details = NULL WHERE details IS NULL;


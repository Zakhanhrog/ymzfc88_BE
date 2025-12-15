-- Fix user_payment_methods table to support E_WALLET type
-- This script updates the type column to support the new E_WALLET enum value
-- JPA uses @Enumerated(EnumType.STRING) so type should be VARCHAR, not ENUM

-- Step 1: Convert type column from ENUM to VARCHAR (if it's currently ENUM)
-- This will work even if the column is already VARCHAR
ALTER TABLE user_payment_methods 
MODIFY COLUMN type VARCHAR(20) NOT NULL;

-- Step 2: Add phone_number column if it doesn't exist
-- MySQL doesn't support IF NOT EXISTS in ALTER TABLE, so we use a workaround
SET @dbname = DATABASE();
SET @tablename = 'user_payment_methods';
SET @columnname = 'phone_number';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) NOT NULL DEFAULT ""')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Step 3: Update account_number column length to 60
ALTER TABLE user_payment_methods 
MODIFY COLUMN account_number VARCHAR(60) NOT NULL;

-- Step 4: For existing records without phone_number, set empty string
UPDATE user_payment_methods 
SET phone_number = '' 
WHERE phone_number IS NULL OR phone_number = '';


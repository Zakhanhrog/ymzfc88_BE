-- Fix point_transactions table to support longer enum values
-- The WITHDRAW_DEDUCTION enum value is too long for current column

-- Check current column definition
-- SHOW COLUMNS FROM point_transactions WHERE Field = 'type';

-- Update the type column to support longer enum values
ALTER TABLE point_transactions 
MODIFY COLUMN type ENUM(
    'EARN',
    'SPEND', 
    'ADMIN_ADD',
    'ADMIN_SUBTRACT',
    'DEPOSIT_BONUS',
    'WITHDRAW_DEDUCTION',
    'REFUND'
) NOT NULL;

-- Verify the change
-- SHOW COLUMNS FROM point_transactions WHERE Field = 'type';

-- Alternative if the above doesn't work (change to VARCHAR):
-- ALTER TABLE point_transactions MODIFY COLUMN type VARCHAR(50) NOT NULL;

-- Check if there are any existing data that might conflict
-- SELECT DISTINCT type FROM point_transactions;
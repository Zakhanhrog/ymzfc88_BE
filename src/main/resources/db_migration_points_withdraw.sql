-- Migration script to support points withdrawal feature
-- Add new point transaction type for withdraw deduction

-- This script is informational only since we're using JPA with DDL auto-update
-- The new enum value WITHDRAW_DEDUCTION will be automatically handled by JPA

-- If manually managing database schema, run this:
-- ALTER TABLE point_transactions MODIFY COLUMN type ENUM('EARN', 'SPEND', 'ADMIN_ADD', 'ADMIN_SUBTRACT', 'DEPOSIT_BONUS', 'WITHDRAW_DEDUCTION', 'REFUND');

-- Note: Make sure your database supports the new enum value
-- For MySQL/MariaDB, the enum constraint will be automatically updated by JPA
-- For PostgreSQL, you might need to manually add the new enum value:
-- ALTER TYPE point_transaction_type ADD VALUE 'WITHDRAW_DEDUCTION';

-- Example queries to verify the feature works:

-- Check user points before withdraw
-- SELECT u.username, up.total_points, up.lifetime_earned, up.lifetime_spent 
-- FROM user_points up 
-- JOIN users u ON up.user_id = u.id 
-- WHERE u.username = 'test_user';

-- Check point transactions after withdraw
-- SELECT pt.transaction_code, pt.type, pt.points, pt.balance_before, pt.balance_after, pt.description, pt.created_at
-- FROM point_transactions pt
-- JOIN users u ON pt.user_id = u.id
-- WHERE u.username = 'test_user'
-- ORDER BY pt.created_at DESC
-- LIMIT 10;

-- Check withdraw transactions with points info
-- SELECT t.transaction_code, t.amount, t.note, t.created_at
-- FROM transactions t
-- JOIN users u ON t.user_id = u.id
-- WHERE u.username = 'test_user' 
-- AND t.type = 'WITHDRAW'
-- AND t.note LIKE '%Points:%'
-- ORDER BY t.created_at DESC;
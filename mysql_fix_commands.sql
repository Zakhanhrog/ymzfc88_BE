-- Câu lệnh SQL để fix lỗi point_transactions table
-- Chạy trực tiếp trong MySQL command line hoặc MySQL Workbench

-- 1. Kiểm tra cấu trúc hiện tại của column type
SHOW COLUMNS FROM point_transactions WHERE Field = 'type';

-- 2. Fix column type để hỗ trợ enum dài hơn
ALTER TABLE point_transactions 
MODIFY COLUMN type VARCHAR(50) NOT NULL;

-- 3. Kiểm tra lại sau khi fix
SHOW COLUMNS FROM point_transactions WHERE Field = 'type';

-- 4. Xem dữ liệu hiện có (nếu có)
SELECT DISTINCT type FROM point_transactions;

-- 5. Thêm index nếu cần (optional)
-- CREATE INDEX idx_point_transactions_type ON point_transactions(type);

-- Hoặc nếu bạn muốn giữ ENUM thay vì VARCHAR:
-- ALTER TABLE point_transactions 
-- MODIFY COLUMN type ENUM(
--     'EARN',
--     'SPEND', 
--     'ADMIN_ADD',
--     'ADMIN_SUBTRACT',
--     'DEPOSIT_BONUS',
--     'WITHDRAW_DEDUCTION',
--     'REFUND'
-- ) NOT NULL;
-- Cập nhật bảng transactions để hỗ trợ upload ảnh bill
-- Chạy script này để update database

-- Thêm các cột mới cho ảnh bill
ALTER TABLE transactions 
MODIFY COLUMN bill_image LONGTEXT COMMENT 'Base64 data của ảnh bill',
ADD COLUMN IF NOT EXISTS bill_image_name VARCHAR(255) NULL COMMENT 'Tên file ảnh gốc' AFTER bill_image,
ADD COLUMN IF NOT EXISTS bill_image_url VARCHAR(500) NULL COMMENT 'URL của file ảnh đã lưu' AFTER bill_image_name;

-- Tạo index để tối ưu performance
CREATE INDEX IF NOT EXISTS idx_transactions_status_type ON transactions(status, type);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_user_status ON transactions(user_id, status);

-- Kiểm tra kết quả
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'transactions' 
AND COLUMN_NAME IN ('bill_image', 'bill_image_name', 'bill_image_url')
ORDER BY ORDINAL_POSITION;
-- Script để cập nhật database hỗ trợ UTF8 cho emoji
-- Chạy script này trên MySQL database

-- 1. Cập nhật database charset
ALTER DATABASE loto79_db CHARACTER SET utf8 COLLATE utf8_unicode_ci;

-- 2. Cập nhật bảng marquee_notifications nếu đã tồn tại
ALTER TABLE marquee_notifications CONVERT TO CHARACTER SET utf8 COLLATE utf8_unicode_ci;

-- 3. Cập nhật cột content để hỗ trợ UTF8
ALTER TABLE marquee_notifications MODIFY COLUMN content TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci;

-- 4. Kiểm tra charset của database
SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME 
FROM information_schema.SCHEMATA 
WHERE SCHEMA_NAME = 'loto79_db';

-- 5. Kiểm tra charset của bảng
SELECT TABLE_NAME, TABLE_COLLATION 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'loto79_db' AND TABLE_NAME = 'marquee_notifications';

-- 6. Kiểm tra charset của cột content
SELECT COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'loto79_db' 
AND TABLE_NAME = 'marquee_notifications' 
AND COLUMN_NAME = 'content';

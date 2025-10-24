-- Script test emoji support
-- Chạy script này để test emoji trong database

-- 1. Test tạo bảng với UTF8
CREATE TABLE IF NOT EXISTS test_emoji (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci
);

-- 2. Test insert emoji
INSERT INTO test_emoji (content) VALUES 
('🎉 CHÀO MỪNG ĐẾN VỚI AE888! 🥳'),
('🔥 HOT DEAL - TẶNG NGAY 100% TIỀN NẠP! 💰'),
('🎰 50 FREE SPIN ĐANG CHỜ BẠN! 🎁'),
('⚠️ Hệ thống bảo trì từ 02:00-04:00 ⏰'),
('🏆 KẾT QUẢ XỔ SỐ: 12345-67890-11111 🎯');

-- 3. Test select emoji
SELECT * FROM test_emoji;

-- 4. Kiểm tra charset
SELECT 
    TABLE_NAME,
    TABLE_COLLATION,
    CHARACTER_SET_NAME
FROM information_schema.TABLES t
JOIN information_schema.COLLATIONS c ON t.TABLE_COLLATION = c.COLLATION_NAME
WHERE TABLE_SCHEMA = 'loto79_db' AND TABLE_NAME = 'test_emoji';

-- 5. Cleanup
DROP TABLE test_emoji;

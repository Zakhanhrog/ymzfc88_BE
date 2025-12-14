-- =====================================================
-- SCRIPT RESET ADMIN USER
-- =====================================================
-- Script này xóa admin user cũ để backend có thể tự tạo lại
-- với password hash chính xác
-- 
-- CÁCH SỬ DỤNG:
-- 1. Chạy script này để xóa admin user cũ
-- 2. Chạy backend (Spring Boot)
-- 3. DataInitializer sẽ tự động tạo admin user mới với password hash chính xác
-- 4. Đăng nhập với:
--    - Username: admin
--    - Password: admin123
--    - C2 Password: admin123 (sẽ được tạo khi backend chạy)
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Xóa user_wallet của admin (nếu có)
DELETE FROM user_wallets WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');

-- Xóa admin user
DELETE FROM users WHERE username = 'admin';

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ Đã xóa admin user cũ. Bây giờ hãy chạy backend để tự động tạo admin user mới với password hash chính xác!' AS status;


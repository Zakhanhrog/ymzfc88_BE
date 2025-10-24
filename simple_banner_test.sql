-- Script test Banner đơn giản
-- Chạy script này để tạo dữ liệu test

-- 1. Xóa bảng cũ nếu có
DROP TABLE IF EXISTS banners;

-- 2. Tạo bảng banners đơn giản
CREATE TABLE banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    banner_type ENUM('MAIN_BANNER', 'SIDEBAR_BANNER') NOT NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. Insert dữ liệu test - Banner chính (5 cái)
INSERT INTO banners (image_url, banner_type, display_order, is_active) VALUES 
('https://via.placeholder.com/800x200/FF0000/FFFFFF?text=MAIN+BANNER+1', 'MAIN_BANNER', 1, TRUE),
('https://via.placeholder.com/800x200/00AA00/FFFFFF?text=MAIN+BANNER+2', 'MAIN_BANNER', 2, TRUE),
('https://via.placeholder.com/800x200/0066CC/FFFFFF?text=MAIN+BANNER+3', 'MAIN_BANNER', 3, TRUE),
('https://via.placeholder.com/800x200/FF6600/FFFFFF?text=MAIN+BANNER+4', 'MAIN_BANNER', 4, TRUE),
('https://via.placeholder.com/800x200/9900CC/FFFFFF?text=MAIN+BANNER+5', 'MAIN_BANNER', 5, TRUE);

-- 4. Insert dữ liệu test - Banner sidebar (3 cái)
INSERT INTO banners (image_url, banner_type, display_order, is_active) VALUES 
('https://via.placeholder.com/300x400/FF0000/FFFFFF?text=SIDEBAR+1', 'SIDEBAR_BANNER', 1, TRUE),
('https://via.placeholder.com/300x400/00AA00/FFFFFF?text=SIDEBAR+2', 'SIDEBAR_BANNER', 2, TRUE),
('https://via.placeholder.com/300x400/0066CC/FFFFFF?text=SIDEBAR+3', 'SIDEBAR_BANNER', 3, TRUE);

-- 5. Kiểm tra dữ liệu
SELECT * FROM banners ORDER BY banner_type, display_order;

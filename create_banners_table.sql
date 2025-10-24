-- Script tạo bảng banners
-- Chạy script này sau khi backend đã khởi động

CREATE TABLE IF NOT EXISTS banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci,
    image_url VARCHAR(500) NOT NULL,
    link_url VARCHAR(500),
    banner_type ENUM('MAIN_BANNER', 'SIDEBAR_BANNER', 'PROMOTION_BANNER') NOT NULL,
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATETIME,
    end_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Tạo index để tối ưu query
CREATE INDEX idx_banners_type_active ON banners(banner_type, is_active);
CREATE INDEX idx_banners_display_order ON banners(display_order);
CREATE INDEX idx_banners_dates ON banners(start_date, end_date);

-- Insert dữ liệu mẫu
INSERT INTO banners (
    title, 
    description, 
    image_url, 
    link_url, 
    banner_type, 
    display_order, 
    is_active, 
    created_by
) VALUES 
(
    'Banner chính AE888',
    'Banner quảng cáo chính cho AE888',
    'https://via.placeholder.com/800x200/FF0000/FFFFFF?text=MAIN+BANNER+AE888',
    'https://ae888.com',
    'MAIN_BANNER',
    1,
    TRUE,
    'admin'
),
(
    'Banner sidebar thể thao',
    'Banner quảng cáo thể thao',
    'https://via.placeholder.com/300x400/0066CC/FFFFFF?text=SPORTS+BANNER',
    'https://ae888.com/sports',
    'SIDEBAR_BANNER',
    1,
    TRUE,
    'admin'
),
(
    'Banner khuyến mãi',
    'Banner khuyến mãi đặc biệt',
    'https://via.placeholder.com/600x200/00AA00/FFFFFF?text=PROMOTION+BANNER',
    'https://ae888.com/promotions',
    'PROMOTION_BANNER',
    1,
    TRUE,
    'admin'
);

-- Kiểm tra dữ liệu
SELECT * FROM banners;

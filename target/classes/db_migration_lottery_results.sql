-- Migration script: Tạo bảng lottery_results và thêm province vào bảng bets
-- Chạy script này để cập nhật database

-- 1. Tạo bảng lottery_results để lưu kết quả xổ số
CREATE TABLE IF NOT EXISTS lottery_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region VARCHAR(50) NOT NULL COMMENT 'Vùng miền: mienBac, mienTrungNam',
    province VARCHAR(50) NULL COMMENT 'Tỉnh (chỉ cho Miền Trung Nam): gialai, binhduong, ninhthuan, travinh, vinhlong',
    draw_date DATE NOT NULL COMMENT 'Ngày quay thưởng',
    results TEXT NOT NULL COMMENT 'Kết quả các giải (JSON format)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'Trạng thái: DRAFT, PUBLISHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Unique constraint: mỗi ngày chỉ có 1 kết quả cho mỗi region/province
    UNIQUE KEY unique_result (region, province, draw_date),
    
    -- Index để tìm kiếm nhanh
    INDEX idx_region (region),
    INDEX idx_province (province),
    INDEX idx_draw_date (draw_date),
    INDEX idx_status (status),
    INDEX idx_region_province (region, province),
    INDEX idx_published_results (region, province, draw_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng lưu kết quả xổ số - Admin nhập thủ công';

-- 2. Thêm cột province vào bảng bets (nếu chưa có)
ALTER TABLE bets 
ADD COLUMN IF NOT EXISTS province VARCHAR(50) NULL 
COMMENT 'Tỉnh (chỉ cho Miền Trung Nam): gialai, binhduong, ninhthuan, travinh, vinhlong'
AFTER region;

-- 3. Tạo index cho province trong bets
CREATE INDEX IF NOT EXISTS idx_bets_province ON bets(province);
CREATE INDEX IF NOT EXISTS idx_bets_region_province ON bets(region, province);

-- 4. Insert mẫu kết quả Miền Bắc (cho testing)
-- Lưu ý: Uncomment dòng dưới nếu muốn insert data mẫu
/*
INSERT INTO lottery_results (region, province, draw_date, results, status) VALUES
('mienBac', NULL, '2025-10-12', '{
  "dac-biet": "00943",
  "giai-nhat": "43213",
  "giai-nhi": ["66146", "15901"],
  "giai-ba": ["22906", "04955", "93893", "32538", "25660", "85773"],
  "giai-tu": ["8964", "0803", "4867", "2405"],
  "giai-nam": ["9122", "6281", "8813", "6672", "8101", "7293"],
  "giai-sau": ["803", "301", "325"],
  "giai-bay": ["84", "09", "69", "79"]
}', 'PUBLISHED');
*/

-- 5. Insert mẫu kết quả Gia Lai (cho testing)
/*
INSERT INTO lottery_results (region, province, draw_date, results, status) VALUES
('mienTrungNam', 'gialai', '2025-10-12', '{
  "dac-biet": "042293",
  "giai-nhat": "02518",
  "giai-nhi": ["49226"],
  "giai-ba": ["03856", "04216"],
  "giai-tu": ["00810", "02321", "00681", "51728", "24507", "58068", "96136"],
  "giai-nam": ["8877"],
  "giai-sau": ["5934", "7442", "3430"],
  "giai-bay": ["884"],
  "giai-tam": ["40"]
}', 'PUBLISHED');
*/

-- Migration completed successfully


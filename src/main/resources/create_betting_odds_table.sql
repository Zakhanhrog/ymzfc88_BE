-- =====================================================
-- Script tạo bảng betting_odds
-- =====================================================
-- Tạo bảng để quản lý tỷ lệ cược cho các loại hình xổ số
-- =====================================================

CREATE TABLE IF NOT EXISTS betting_odds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID tự tăng',
    bet_name VARCHAR(200) NOT NULL COMMENT 'Tên hiển thị của loại cược',
    bet_type VARCHAR(100) NOT NULL COMMENT 'Loại cược (ví dụ: loto2s, loto-xien-2, dac-biet, etc.)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    description VARCHAR(500) COMMENT 'Mô tả loại cược',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Trạng thái active/inactive',
    odds INT NOT NULL COMMENT 'Tỷ lệ cược (1 ăn bao nhiêu)',
    price_per_point INT NOT NULL COMMENT 'Đơn giá 1 điểm (đơn vị: VNĐ)',
    region VARCHAR(50) NOT NULL COMMENT 'Khu vực: MIEN_BAC hoặc MIEN_TRUNG_NAM',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    
    -- Constraints
    CONSTRAINT unique_region_bet_type UNIQUE (region, bet_type),
    CONSTRAINT check_odds_positive CHECK (odds > 0),
    CONSTRAINT check_price_per_point_positive CHECK (price_per_point > 0),
    CONSTRAINT check_region_valid CHECK (region IN ('MIEN_BAC', 'MIEN_TRUNG_NAM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý tỷ lệ cược xổ số';

-- Tạo indexes để tối ưu performance
CREATE INDEX idx_betting_odds_region ON betting_odds(region);
CREATE INDEX idx_betting_odds_bet_type ON betting_odds(bet_type);
CREATE INDEX idx_betting_odds_is_active ON betting_odds(is_active);
CREATE INDEX idx_betting_odds_created_at ON betting_odds(created_at);

-- Hiển thị thông tin bảng đã tạo
DESCRIBE betting_odds;

-- =====================================================
-- THÊM DỮ LIỆU MẶC ĐỊNH CHO MIỀN BẮC
-- =====================================================

-- Xóa dữ liệu cũ của Miền Bắc (nếu có)
DELETE FROM betting_odds WHERE region = 'MIEN_BAC';

-- Thêm dữ liệu mới cho Miền Bắc
INSERT INTO betting_odds (region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at) VALUES

-- ========== NHÓM 1: LOTO CƠ BẢN ==========
('MIEN_BAC', 'loto2s', 'Loto2s', 'Lô 2 số', 80, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-2', 'Loto xiên 2', 'Xiên 2 số', 15, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-3', 'Loto xiên 3', 'Xiên 3 số', 60, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-4', 'Loto xiên 4', 'Xiên 4 số', 300, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-3s', 'Loto 3s', 'Lô 3 số', 500, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-4s', 'Loto 4s', 'Lô 4 số', 5000, 1000, true, NOW(), NOW()),

-- ========== NHÓM 2: ĐỀ CÁC GIẢI ==========
('MIEN_BAC', 'giai-nhat', 'Giải nhất', 'Đề giải nhất', 90, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'dac-biet', 'Đặc biệt', 'Đề đặc biệt', 95, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'dau-dac-biet', 'Đầu Đặc biệt', 'Đề đầu đặc biệt', 9, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'de-giai-7', 'Đề giải 7', 'Đề giải 7', 23, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'dau-duoi', 'Đầu / đuôi', 'Đầu đuôi', 4, 1000, true, NOW(), NOW()),

-- ========== NHÓM 3: 3 SỐ VÀ 4 SỐ ĐẶC BIỆT ==========
('MIEN_BAC', '3s-giai-nhat', '3s giải nhất', '3 số giải nhất', 600, 1000, true, NOW(), NOW()),
('MIEN_BAC', '3s-giai-6', '3s giải 6', '3 số giải 6', 600, 1000, true, NOW(), NOW()),
('MIEN_BAC', '3s-dau-duoi', '3s đầu đuôi', '3 số đầu đuôi', 600, 1000, true, NOW(), NOW()),
('MIEN_BAC', '3s-dac-biet', '3s đặc biệt', '3 số đặc biệt', 650, 1000, true, NOW(), NOW()),
('MIEN_BAC', '4s-dac-biet', '4s đặc biệt', '4 số đặc biệt', 6500, 1000, true, NOW(), NOW()),

-- ========== NHÓM 4: LOTO TRƯỢT (Chọn N số, chỉ cần N-1 số về là thắng) ==========
('MIEN_BAC', 'loto-truot-4', 'Loto trượt 4', 'Lô trượt 4 - Chọn 4 số, chỉ cần 3 số về', 2, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-5', 'Loto trượt 5', 'Lô trượt 5 - Chọn 5 số, chỉ cần 4 số về', 3, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-6', 'Loto trượt 6', 'Lô trượt 6 - Chọn 6 số, chỉ cần 5 số về', 5, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-7', 'Loto trượt 7', 'Lô trượt 7 - Chọn 7 số, chỉ cần 6 số về', 8, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-8', 'Loto trượt 8', 'Lô trượt 8 - Chọn 8 số, chỉ cần 7 số về', 12, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-9', 'Loto trượt 9', 'Lô trượt 9 - Chọn 9 số, chỉ cần 8 số về', 18, 1000, true, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-10', 'Loto trượt 10', 'Lô trượt 10 - Chọn 10 số, chỉ cần 9 số về', 25, 1000, true, NOW(), NOW());

-- Xác nhận số lượng bản ghi đã thêm
SELECT COUNT(*) as total_records FROM betting_odds WHERE region = 'MIEN_BAC';

-- Hiển thị tất cả dữ liệu vừa thêm
SELECT * FROM betting_odds WHERE region = 'MIEN_BAC' ORDER BY bet_type;

-- =====================================================
-- THÊM DỮ LIỆU MẶC ĐỊNH CHO MIỀN TRUNG & NAM
-- =====================================================

-- Xóa dữ liệu cũ của Miền Trung & Nam (nếu có)
DELETE FROM betting_odds WHERE region = 'MIEN_TRUNG_NAM';

-- Thêm dữ liệu mới cho Miền Trung & Nam
INSERT INTO betting_odds (region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at) VALUES

-- ========== NHÓM 1: LOTO CƠ BẢN ==========
('MIEN_TRUNG_NAM', 'loto-2-so', 'Loto 2 số', 'Lô 2 số', 80, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-2', 'Loto xiên 2', 'Xiên 2 số', 15, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-3', 'Loto xiên 3', 'Xiên 3 số', 60, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-4', 'Loto xiên 4', 'Xiên 4 số', 300, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-3s', 'Loto 3s', 'Lô 3 số', 500, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-4s', 'Loto 4s', 'Lô 4 số', 5000, 1000, true, NOW(), NOW()),

-- ========== NHÓM 2: ĐỀ CÁC GIẢI ==========
('MIEN_TRUNG_NAM', 'dac-biet', 'Đặc biệt', 'Đề đặc biệt', 95, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dau-duoi', 'Đầu / đuôi', 'Đầu đuôi', 4, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dau-dac-biet', 'Đầu đặc biệt', 'Đề đầu đặc biệt', 9, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'de-giai-8', 'Đề giải 8', 'Đề giải 8', 23, 1000, true, NOW(), NOW()),

-- ========== NHÓM 3: 3 SỐ VÀ 4 SỐ ĐẶC BIỆT ==========
('MIEN_TRUNG_NAM', '3s-giai-7', '3s giải 7', '3 số giải 7', 600, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-dau-duoi', '3s đầu đuôi', '3 số đầu đuôi', 600, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-dac-biet', '3s đặc biệt', '3 số đặc biệt', 650, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', '4s-dac-biet', '4s đặc biệt', '4 số đặc biệt', 6500, 1000, true, NOW(), NOW()),

-- ========== NHÓM 4: LOTO TRƯỢT (Chọn N số, chỉ cần N-1 số về là thắng) ==========
('MIEN_TRUNG_NAM', 'loto-truot-4', 'Loto trượt 4', 'Lô trượt 4 - Chọn 4 số, chỉ cần 3 số về', 2, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-5', 'Loto trượt 5', 'Lô trượt 5 - Chọn 5 số, chỉ cần 4 số về', 3, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-6', 'Loto trượt 6', 'Lô trượt 6 - Chọn 6 số, chỉ cần 5 số về', 5, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-7', 'Loto trượt 7', 'Lô trượt 7 - Chọn 7 số, chỉ cần 6 số về', 8, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-8', 'Loto trượt 8', 'Lô trượt 8 - Chọn 8 số, chỉ cần 7 số về', 12, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-9', 'Loto trượt 9', 'Lô trượt 9 - Chọn 9 số, chỉ cần 8 số về', 18, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-10', 'Loto trượt 10', 'Lô trượt 10 - Chọn 10 số, chỉ cần 9 số về', 25, 1000, true, NOW(), NOW());

-- Xác nhận số lượng bản ghi Miền Trung & Nam
SELECT COUNT(*) as total_records_mien_trung_nam FROM betting_odds WHERE region = 'MIEN_TRUNG_NAM';

-- Hiển thị tất cả dữ liệu Miền Trung & Nam
SELECT * FROM betting_odds WHERE region = 'MIEN_TRUNG_NAM' ORDER BY bet_type;

-- Tổng kết toàn bộ dữ liệu
SELECT 
    region,
    COUNT(*) as total_records,
    MIN(odds) as min_odds,
    MAX(odds) as max_odds,
    AVG(odds) as avg_odds
FROM betting_odds 
GROUP BY region 
ORDER BY region;

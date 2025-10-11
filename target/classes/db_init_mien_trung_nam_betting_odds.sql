-- =====================================================
-- Dữ liệu khởi tạo cho bảng betting_odds - Miền Trung & Nam
-- =====================================================
-- Script SQL để thêm tất cả các loại cược Miền Trung & Nam vào database
-- Cấu trúc bảng: betting_odds
-- Fields: id, region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at
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
('MIEN_TRUNG_NAM', 'dau-duoi-mien-trung-nam', 'Đầu / đuôi', 'Đầu đuôi', 4, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dau-dac-biet', 'Đầu đặc biệt', 'Đề đầu đặc biệt', 9, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'de-giai-8', 'Đề giải 8', 'Đề giải 8', 23, 1000, true, NOW(), NOW()),

-- ========== NHÓM 3: 3 SỐ VÀ 4 SỐ ĐẶC BIỆT ==========
('MIEN_TRUNG_NAM', '3s-giai-7', '3s giải 7', '3 số giải 7', 600, 1000, true, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-dau-duoi-mien-trung-nam', '3s đầu đuôi', '3 số đầu đuôi', 600, 1000, true, NOW(), NOW()),
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

-- Xác nhận số lượng bản ghi đã thêm
SELECT COUNT(*) as total_records FROM betting_odds WHERE region = 'MIEN_TRUNG_NAM';

-- Hiển thị tất cả dữ liệu vừa thêm
SELECT * FROM betting_odds WHERE region = 'MIEN_TRUNG_NAM' ORDER BY bet_type;

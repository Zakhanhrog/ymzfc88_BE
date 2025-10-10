-- =====================================================
-- Dữ liệu khởi tạo cho bảng betting_odds - Miền Bắc
-- =====================================================
-- Script SQL để thêm tất cả các loại cược Miền Bắc vào database
-- Cấu trúc bảng: betting_odds
-- Fields: id, region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at
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


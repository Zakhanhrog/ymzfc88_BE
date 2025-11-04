-- MySQL INSERT statements for betting odds data
-- Table: betting_odds
-- Columns: id (auto), region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at

-- Miền Bắc betting odds
INSERT INTO betting_odds (region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at) VALUES
('MIEN_BAC', 'loto2s', 'Loto 2s', 'Lô 2 số truyền thống (Miền Bắc)', 80, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-2', 'Loto xiên 2', 'Chọn 2 số, trúng cả 2 (Miền Bắc)', 10, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-3', 'Loto xiên 3', 'Chọn 3 số, trúng cả 3 (Miền Bắc)', 40, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-xien-4', 'Loto xiên 4', 'Chọn 4 số, trúng cả 4 (Miền Bắc)', 150, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-3s', 'Loto 3s', 'Lô 3 số (Miền Bắc)', 600, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-4s', 'Loto 4s', 'Lô 4 số (Miền Bắc)', 4000, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'giai-nhat', 'Giải nhất', 'Đề giải nhất (Miền Bắc)', 95, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'de-giai-7', 'Đề giải 7', 'Đề giải 7 (×4 hiển thị), Miền Bắc', 20, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'dac-biet', 'Đặc biệt', 'Đề đặc biệt (Miền Bắc)', 95, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'dau-dac-biet', 'Đầu Đặc biệt', 'Đề đầu đặc biệt (Miền Bắc)', 9, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'dau-duoi', 'Đầu / đuôi', 'Đầu/đuôi (Miền Bắc)', 4, 1000, 1, NOW(), NOW()),
('MIEN_BAC', '3s-giai-nhat', '3s giải nhất', '3 số giải nhất (Miền Bắc)', 600, 1000, 1, NOW(), NOW()),
('MIEN_BAC', '3s-giai-6', '3s giải 6', '3 số giải 6 (×3 hiển thị), Miền Bắc', 300, 1000, 1, NOW(), NOW()),
('MIEN_BAC', '3s-dau-duoi', '3s đầu đuôi', '3 số đầu đuôi (Miền Bắc)', 120, 1000, 1, NOW(), NOW()),
('MIEN_BAC', '3s-dac-biet', '3s đặc biệt', '3 số đặc biệt (Miền Bắc)', 650, 1000, 1, NOW(), NOW()),
('MIEN_BAC', '4s-dac-biet', '4s đặc biệt', '4 số đặc biệt (Miền Bắc)', 4500, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-4', 'Loto trượt 4', 'Trượt 4 số (Miền Bắc)', 2, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-8', 'Loto trượt 8', 'Trượt 8 số (Miền Bắc)', 4, 1000, 1, NOW(), NOW()),
('MIEN_BAC', 'loto-truot-10', 'Loto trượt 10', 'Trượt 10 số (Miền Bắc)', 6, 1000, 1, NOW(), NOW());

-- Miền Trung Nam betting odds
INSERT INTO betting_odds (region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at) VALUES
('MIEN_TRUNG_NAM', 'loto-2-so', 'Loto 2 số', 'Lô 2 số (Miền Trung & Nam)', 70, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-2', 'Loto xiên 2', 'Chọn 2 số, trúng cả 2 (Miền Trung & Nam)', 9, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-3', 'Loto xiên 3', 'Chọn 3 số, trúng cả 3 (Miền Trung & Nam)', 35, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-xien-4', 'Loto xiên 4', 'Chọn 4 số, trúng cả 4 (Miền Trung & Nam)', 120, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-3s', 'Loto 3s', 'Lô 3 số (Miền Trung & Nam)', 550, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-4s', 'Loto 4s', 'Lô 4 số (Miền Trung & Nam)', 3800, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dac-biet', 'Đặc biệt', 'Đề đặc biệt (Miền Trung & Nam)', 90, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dau-duoi-mien-trung-nam', 'Đầu / đuôi', 'Đầu đuôi (×2 hiển thị), Miền Trung & Nam', 2, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'dau-dac-biet', 'Đầu đặc biệt', 'Đề đầu đặc biệt (Miền Trung & Nam)', 9, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'de-giai-8', 'Đề giải 8', 'Đề giải 8 (Miền Trung & Nam)', 18, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-giai-7', '3s giải 7', '3 số giải 7 (Miền Trung & Nam)', 300, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-dau-duoi-mien-trung-nam', '3s đầu đuôi', '3 số đầu đuôi (×2 hiển thị), Miền Trung & Nam', 100, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', '3s-dac-biet', '3s đặc biệt', '3 số đặc biệt (Miền Trung & Nam)', 600, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', '4s-dac-biet', '4s đặc biệt', '4 số đặc biệt (Miền Trung & Nam)', 4200, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-4', 'Loto trượt 4', 'Trượt 4 số (Miền Trung & Nam)', 2, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-8', 'Loto trượt 8', 'Trượt 8 số (Miền Trung & Nam)', 4, 1000, 1, NOW(), NOW()),
('MIEN_TRUNG_NAM', 'loto-truot-10', 'Loto trượt 10', 'Trượt 10 số (Miền Trung & Nam)', 5, 1000, 1, NOW(), NOW());

-- Verify the data
SELECT COUNT(*) as total_records FROM betting_odds;
SELECT region, COUNT(*) as count_per_region FROM betting_odds GROUP BY region;

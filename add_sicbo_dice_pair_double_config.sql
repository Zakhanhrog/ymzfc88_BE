-- Migration: Thêm config cho Sicbo dice pair double bets (cặp đôi)
-- 6 cặp đôi: 11, 22, 33, 44, 55, 66 với tỷ lệ cược: 1:8

INSERT IGNORE INTO sicbo_quick_bet_config (code, name, description, payout_multiplier, fee_rate, layout_group, display_order, is_active, created_at, updated_at)
VALUES
('sicbo_pair_double_1', 'Cặp đôi 1-1', 'Cược cặp đôi 1-1', 8.00, NULL, 'DICE_PAIR_DOUBLE', 0, 1, NOW(), NOW()),
('sicbo_pair_double_2', 'Cặp đôi 2-2', 'Cược cặp đôi 2-2', 8.00, NULL, 'DICE_PAIR_DOUBLE', 1, 1, NOW(), NOW()),
('sicbo_pair_double_3', 'Cặp đôi 3-3', 'Cược cặp đôi 3-3', 8.00, NULL, 'DICE_PAIR_DOUBLE', 2, 1, NOW(), NOW()),
('sicbo_pair_double_4', 'Cặp đôi 4-4', 'Cược cặp đôi 4-4', 8.00, NULL, 'DICE_PAIR_DOUBLE', 3, 1, NOW(), NOW()),
('sicbo_pair_double_5', 'Cặp đôi 5-5', 'Cược cặp đôi 5-5', 8.00, NULL, 'DICE_PAIR_DOUBLE', 4, 1, NOW(), NOW()),
('sicbo_pair_double_6', 'Cặp đôi 6-6', 'Cược cặp đôi 6-6', 8.00, NULL, 'DICE_PAIR_DOUBLE', 5, 1, NOW(), NOW());

-- Lưu ý: Tất cả các cặp đôi đều có:
-- - payout_multiplier = 8.00 (tỷ lệ 1:8)
-- - fee_rate = NULL (không tính phế riêng, sẽ lấy theo cấu hình bàn)
-- - layout_group = 'DICE_PAIR_DOUBLE' (nhóm riêng cho cặp đôi)
-- - is_active = 1 (đang hoạt động, dùng 1 thay vì true cho MySQL)
-- - INSERT IGNORE: Nếu đã tồn tại (duplicate key) thì không insert lại và không báo lỗi


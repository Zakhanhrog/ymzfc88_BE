-- Migration: Thêm config cho Sicbo dice pair bets
-- Tất cả 15 cặp xúc xắc đều có cùng tỷ lệ cược: 1:5

INSERT INTO sicbo_quick_bet_config (code, name, description, payout_multiplier, fee_rate, layout_group, display_order, is_active, created_at, updated_at)
VALUES
-- Row 1: 7 pairs
('sicbo_pair_1_2', 'Cặp 1-2', 'Cược cặp xúc xắc 1 và 2', 5.00, NULL, 'DICE_PAIR', 1, true, NOW(), NOW()),
('sicbo_pair_1_3', 'Cặp 1-3', 'Cược cặp xúc xắc 1 và 3', 5.00, NULL, 'DICE_PAIR', 2, true, NOW(), NOW()),
('sicbo_pair_1_4', 'Cặp 1-4', 'Cược cặp xúc xắc 1 và 4', 5.00, NULL, 'DICE_PAIR', 3, true, NOW(), NOW()),
('sicbo_pair_1_5', 'Cặp 1-5', 'Cược cặp xúc xắc 1 và 5', 5.00, NULL, 'DICE_PAIR', 4, true, NOW(), NOW()),
('sicbo_pair_1_6', 'Cặp 1-6', 'Cược cặp xúc xắc 1 và 6', 5.00, NULL, 'DICE_PAIR', 5, true, NOW(), NOW()),
('sicbo_pair_2_3', 'Cặp 2-3', 'Cược cặp xúc xắc 2 và 3', 5.00, NULL, 'DICE_PAIR', 6, true, NOW(), NOW()),
('sicbo_pair_2_4', 'Cặp 2-4', 'Cược cặp xúc xắc 2 và 4', 5.00, NULL, 'DICE_PAIR', 7, true, NOW(), NOW()),

-- Row 2: 8 pairs
('sicbo_pair_2_5', 'Cặp 2-5', 'Cược cặp xúc xắc 2 và 5', 5.00, NULL, 'DICE_PAIR', 8, true, NOW(), NOW()),
('sicbo_pair_2_6', 'Cặp 2-6', 'Cược cặp xúc xắc 2 và 6', 5.00, NULL, 'DICE_PAIR', 9, true, NOW(), NOW()),
('sicbo_pair_3_4', 'Cặp 3-4', 'Cược cặp xúc xắc 3 và 4', 5.00, NULL, 'DICE_PAIR', 10, true, NOW(), NOW()),
('sicbo_pair_3_5', 'Cặp 3-5', 'Cược cặp xúc xắc 3 và 5', 5.00, NULL, 'DICE_PAIR', 11, true, NOW(), NOW()),
('sicbo_pair_3_6', 'Cặp 3-6', 'Cược cặp xúc xắc 3 và 6', 5.00, NULL, 'DICE_PAIR', 12, true, NOW(), NOW()),
('sicbo_pair_4_5', 'Cặp 4-5', 'Cược cặp xúc xắc 4 và 5', 5.00, NULL, 'DICE_PAIR', 13, true, NOW(), NOW()),
('sicbo_pair_4_6', 'Cặp 4-6', 'Cược cặp xúc xắc 4 và 6', 5.00, NULL, 'DICE_PAIR', 14, true, NOW(), NOW()),
('sicbo_pair_5_6', 'Cặp 5-6', 'Cược cặp xúc xắc 5 và 6', 5.00, NULL, 'DICE_PAIR', 15, true, NOW(), NOW());

-- Lưu ý: Tất cả các cặp đều có:
-- - payout_multiplier = 5.00 (tỷ lệ 1:5)
-- - fee_rate = NULL (không tính phế riêng, sẽ lấy theo cấu hình bàn)
-- - layout_group = 'DICE_PAIR' (nhóm riêng cho dice pair)
-- - is_active = true (đang hoạt động)


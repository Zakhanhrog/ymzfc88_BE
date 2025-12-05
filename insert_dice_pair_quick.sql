-- Thêm 15 cấu hình dice pair cho Sicbo
-- Chạy script này trong MySQL

INSERT INTO sicbo_quick_bet_config 
(code, name, description, payout_multiplier, fee_rate, layout_group, display_order, is_active, created_at, updated_at)
VALUES
('sicbo_pair_1_2', 'Cặp 1-2', 'Cược cặp xúc xắc 1 và 2', 5.00, NULL, 'DICE_PAIR', 0, 1, NOW(), NOW()),
('sicbo_pair_1_3', 'Cặp 1-3', 'Cược cặp xúc xắc 1 và 3', 5.00, NULL, 'DICE_PAIR', 1, 1, NOW(), NOW()),
('sicbo_pair_1_4', 'Cặp 1-4', 'Cược cặp xúc xắc 1 và 4', 5.00, NULL, 'DICE_PAIR', 2, 1, NOW(), NOW()),
('sicbo_pair_1_5', 'Cặp 1-5', 'Cược cặp xúc xắc 1 và 5', 5.00, NULL, 'DICE_PAIR', 3, 1, NOW(), NOW()),
('sicbo_pair_1_6', 'Cặp 1-6', 'Cược cặp xúc xắc 1 và 6', 5.00, NULL, 'DICE_PAIR', 4, 1, NOW(), NOW()),
('sicbo_pair_2_3', 'Cặp 2-3', 'Cược cặp xúc xắc 2 và 3', 5.00, NULL, 'DICE_PAIR', 5, 1, NOW(), NOW()),
('sicbo_pair_2_4', 'Cặp 2-4', 'Cược cặp xúc xắc 2 và 4', 5.00, NULL, 'DICE_PAIR', 6, 1, NOW(), NOW()),
('sicbo_pair_2_5', 'Cặp 2-5', 'Cược cặp xúc xắc 2 và 5', 5.00, NULL, 'DICE_PAIR', 7, 1, NOW(), NOW()),
('sicbo_pair_2_6', 'Cặp 2-6', 'Cược cặp xúc xắc 2 và 6', 5.00, NULL, 'DICE_PAIR', 8, 1, NOW(), NOW()),
('sicbo_pair_3_4', 'Cặp 3-4', 'Cược cặp xúc xắc 3 và 4', 5.00, NULL, 'DICE_PAIR', 9, 1, NOW(), NOW()),
('sicbo_pair_3_5', 'Cặp 3-5', 'Cược cặp xúc xắc 3 và 5', 5.00, NULL, 'DICE_PAIR', 10, 1, NOW(), NOW()),
('sicbo_pair_3_6', 'Cặp 3-6', 'Cược cặp xúc xắc 3 và 6', 5.00, NULL, 'DICE_PAIR', 11, 1, NOW(), NOW()),
('sicbo_pair_4_5', 'Cặp 4-5', 'Cược cặp xúc xắc 4 và 5', 5.00, NULL, 'DICE_PAIR', 12, 1, NOW(), NOW()),
('sicbo_pair_4_6', 'Cặp 4-6', 'Cược cặp xúc xắc 4 và 6', 5.00, NULL, 'DICE_PAIR', 13, 1, NOW(), NOW()),
('sicbo_pair_5_6', 'Cặp 5-6', 'Cược cặp xúc xắc 5 và 6', 5.00, NULL, 'DICE_PAIR', 14, 1, NOW(), NOW());

SELECT 'Đã thêm 15 cấu hình dice pair thành công!' AS status;


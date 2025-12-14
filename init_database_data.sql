-- =====================================================
-- SCRIPT KHỞI TẠO DỮ LIỆU BAN ĐẦU CHO HỆ THỐNG
-- =====================================================
-- Script này tạo tất cả dữ liệu ban đầu cần thiết cho hệ thống
-- Chạy script này sau khi database đã được tạo (JPA auto-create tables)
-- 
-- LƯU Ý: 
-- 1. Đảm bảo database đã được tạo và các bảng đã được JPA tạo tự động
-- 2. Password admin được hash bằng BCrypt (password: admin123)
-- 3. Nếu đã có dữ liệu, script sẽ bỏ qua các bản ghi trùng lặp
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

-- =====================================================
-- 1. ADMIN USER
-- =====================================================
-- ⚠️ QUAN TRỌNG: KHÔNG TẠO ADMIN USER Ở ĐÂY!
-- 
-- Admin user sẽ được tự động tạo bởi DataInitializer khi backend chạy
-- với password hash chính xác (BCrypt).
-- 
-- Nếu bạn đã chạy script này và admin user đã được tạo với hash không chính xác,
-- hãy chạy script reset_admin_user.sql để xóa admin user cũ, sau đó chạy backend
-- để DataInitializer tự tạo lại.
-- 
-- Thông tin đăng nhập sau khi backend tạo:
-- - Username: admin
-- - Password: admin123  
-- - C2 Password: admin123 (sẽ được tạo khi backend chạy)
-- - Email: admin@xsecret.com
-- 
-- =====================================================
-- BỎ QUA PHẦN TẠO ADMIN USER - ĐỂ BACKEND TỰ TẠO
-- =====================================================

-- Tạo user_wallet cho admin user
INSERT INTO user_wallets (
    user_id,
    balance,
    total_deposit,
    total_withdraw,
    total_bonus,
    frozen_amount,
    created_at,
    updated_at
) 
SELECT 
    u.id,
    0,
    0,
    0,
    0,
    0,
    NOW(),
    NOW()
FROM users u
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- =====================================================
-- 2. PAYMENT METHODS
-- =====================================================
-- Tạo các phương thức thanh toán mặc định

INSERT INTO payment_methods (
    type,
    name,
    account_number,
    account_name,
    bank_code,
    min_amount,
    max_amount,
    fee_percent,
    fee_fixed,
    processing_time,
    is_active,
    display_order,
    description,
    created_at,
    updated_at
) VALUES
-- MoMo Payment Method
(
    'MOMO',
    'Ví MoMo',
    '0987654321',
    'ADMIN XSECRET',
    NULL,
    10000.00,
    50000000.00,
    0.00,
    0.00,
    '5-15 phút',
    true,
    1,
    'Nạp tiền qua ví MoMo',
    NOW(),
    NOW()
),
-- Bank Transfer Payment Method
(
    'BANK',
    'Chuyển khoản ngân hàng',
    '1234567890',
    'ADMIN XSECRET',
    'VCB',
    50000.00,
    100000000.00,
    0.00,
    0.00,
    '10-30 phút',
    true,
    2,
    'Chuyển khoản qua ngân hàng Vietcombank',
    NOW(),
    NOW()
),
-- USDT Payment Method
(
    'USDT',
    'USDT TRC-20',
    'TRX_WALLET_ADDRESS_HERE',
    'ADMIN XSECRET',
    NULL,
    100000.00,
    200000000.00,
    1.00,
    0.00,
    '15-60 phút',
    true,
    3,
    'Nạp tiền bằng USDT qua mạng TRC-20',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE name = name;

-- =====================================================
-- 3. SYSTEM SETTINGS
-- =====================================================
-- Tạo các cài đặt hệ thống mặc định

INSERT INTO system_settings (setting_key, setting_value, description, category, created_at, updated_at) VALUES
-- Withdrawal settings
('default_withdrawal_lock_reason', 'Tài khoản của bạn đã bị khóa rút tiền do vi phạm chính sách của hệ thống. Vui lòng liên hệ admin để được hỗ trợ.', 'Lý do mặc định khi khóa rút tiền của người dùng', 'WITHDRAWAL', NOW(), NOW()),
('min_withdrawal_amount', '50000', 'Số tiền rút tối thiểu', 'WITHDRAWAL', NOW(), NOW()),
('max_withdrawal_amount', '50000000', 'Số tiền rút tối đa', 'WITHDRAWAL', NOW(), NOW()),

-- Deposit settings
('min_deposit_amount', '50000', 'Số tiền nạp tối thiểu', 'DEPOSIT', NOW(), NOW()),
('max_deposit_amount', '100000000', 'Số tiền nạp tối đa', 'DEPOSIT', NOW(), NOW()),

-- System settings
('system_maintenance_message', 'Hệ thống đang bảo trì. Vui lòng quay lại sau.', 'Thông báo khi hệ thống bảo trì', 'SYSTEM', NOW(), NOW()),

-- Commission settings
('agent_commission_percentage', '5', 'Tỷ lệ hoa hồng mặc định cho đại lý (đơn vị %)', 'COMMISSION', NOW(), NOW()),

-- Contact page links
('contact_livechat_link', '#', 'Link cho thẻ Livechat 24/24', 'CONTACT', NOW(), NOW()),
('contact_facebook_link', '#', 'Link cho thẻ Kênh Facebook', 'CONTACT', NOW(), NOW()),
('contact_messenger_link', '#', 'Link cho thẻ Messenger Facebook', 'CONTACT', NOW(), NOW()),
('contact_telegram_link', '#', 'Link cho thẻ Telegram', 'CONTACT', NOW(), NOW()),
('contact_hotline_link', '#', 'Link cho thẻ Hotline', 'CONTACT', NOW(), NOW()),

-- Game refund settings
('sicbo_refund_win_percentage', '0', 'Tỷ lệ hoàn trả (%) cho lệnh thắng Sicbo', 'GAME_REFUND', NOW(), NOW()),
('sicbo_refund_loss_percentage', '0', 'Tỷ lệ hoàn trả (%) cho lệnh thua Sicbo', 'GAME_REFUND', NOW(), NOW()),
('xocdia_refund_win_percentage', '0', 'Tỷ lệ hoàn trả (%) cho lệnh thắng Xóc Đĩa', 'GAME_REFUND', NOW(), NOW()),
('xocdia_refund_loss_percentage', '0', 'Tỷ lệ hoàn trả (%) cho lệnh thua Xóc Đĩa', 'GAME_REFUND', NOW(), NOW()),
('sicbo_refund_payout_time', '12:00', 'Thời gian chạy hoàn trả Sicbo hằng ngày (HH:mm)', 'GAME_REFUND', NOW(), NOW()),
('xocdia_refund_payout_time', '12:00', 'Thời gian chạy hoàn trả Xóc Đĩa hằng ngày (HH:mm)', 'GAME_REFUND', NOW(), NOW())
ON DUPLICATE KEY UPDATE setting_key = setting_key;

-- =====================================================
-- 4. BETTING ODDS (LOTO)
-- =====================================================
-- Tạo tỷ lệ cược cho Miền Bắc và Miền Trung Nam

INSERT INTO betting_odds (region, bet_type, bet_name, description, odds, price_per_point, is_active, created_at, updated_at) VALUES
-- Miền Bắc betting odds
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
('MIEN_BAC', 'loto-truot-10', 'Loto trượt 10', 'Trượt 10 số (Miền Bắc)', 6, 1000, 1, NOW(), NOW()),

-- Miền Trung Nam betting odds
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
('MIEN_TRUNG_NAM', 'loto-truot-10', 'Loto trượt 10', 'Trượt 10 số (Miền Trung & Nam)', 5, 1000, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE region = region, bet_type = bet_type;

-- =====================================================
-- 5. TELEGRAM CONFIG
-- =====================================================
-- Tạo cấu hình Telegram mặc định

INSERT INTO telegram_config (
    bot_token,
    chat_id,
    enabled,
    description,
    created_at,
    updated_at
) VALUES (
    '8421520125:AAGWiE88hE5q8yRwGTtP2cjbMGiqdiRf_SA',
    '-4906012064',
    true,
    'Default Telegram config migrated from application.properties',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE bot_token = bot_token;

-- =====================================================
-- 6. STREAM CONFIGS
-- =====================================================
-- ⚠️ KHÔNG TẠO STREAM CONFIGS Ở ĐÂY!
-- 
-- Stream configs sẽ được tự động tạo bởi StreamConfigDataInitializer khi backend chạy
-- với logic xử lý dữ liệu trùng lặp tự động.
-- 
-- Nếu có dữ liệu trùng lặp, hãy chạy script fix_duplicate_stream_configs.sql
-- trước khi chạy backend.
-- =====================================================

-- =====================================================
-- 7. SICBO QUICK BET CONFIGS
-- =====================================================
-- Tạo cấu hình quick bet cho game Sicbo

INSERT INTO sicbo_quick_bet_config (
    code,
    name,
    description,
    payout_multiplier,
    fee_rate,
    layout_group,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
-- Primary bets
('sicbo_primary_small', 'Xỉu', NULL, 0.97, NULL, 'PRIMARY', 0, 1, NOW(), NOW()),
('sicbo_primary_big', 'Tài', NULL, 0.97, NULL, 'PRIMARY', 1, 1, NOW(), NOW()),

-- Combination bets (Bộ ba)
('sicbo_combo_triple_1', 'Bộ ba 1', NULL, 20.00, NULL, 'COMBINATION', 0, 1, NOW(), NOW()),
('sicbo_combo_triple_6', 'Bộ ba 6', NULL, 20.00, NULL, 'COMBINATION', 1, 1, NOW(), NOW()),
('sicbo_combo_triple_2', 'Bộ ba 2', NULL, 20.00, NULL, 'COMBINATION', 2, 1, NOW(), NOW()),
('sicbo_combo_triple_5', 'Bộ ba 5', NULL, 20.00, NULL, 'COMBINATION', 3, 1, NOW(), NOW()),
('sicbo_combo_triple_3', 'Bộ ba 3', NULL, 20.00, NULL, 'COMBINATION', 4, 1, NOW(), NOW()),
('sicbo_combo_triple_4', 'Bộ ba 4', NULL, 20.00, NULL, 'COMBINATION', 5, 1, NOW(), NOW()),

-- Total Top (Chẵn + Tổng 4-10)
('sicbo_parity_even', 'Chẵn', NULL, 0.97, NULL, 'TOTAL_TOP', 0, 1, NOW(), NOW()),
('sicbo_total_4', 'Tổng 4', NULL, 30.00, NULL, 'TOTAL_TOP', 1, 1, NOW(), NOW()),
('sicbo_total_5', 'Tổng 5', NULL, 18.00, NULL, 'TOTAL_TOP', 2, 1, NOW(), NOW()),
('sicbo_total_6', 'Tổng 6', NULL, 14.00, NULL, 'TOTAL_TOP', 3, 1, NOW(), NOW()),
('sicbo_total_7', 'Tổng 7', NULL, 12.00, NULL, 'TOTAL_TOP', 4, 1, NOW(), NOW()),
('sicbo_total_8', 'Tổng 8', NULL, 8.00, NULL, 'TOTAL_TOP', 5, 1, NOW(), NOW()),
('sicbo_total_9', 'Tổng 9', NULL, 6.00, NULL, 'TOTAL_TOP', 6, 1, NOW(), NOW()),
('sicbo_total_10', 'Tổng 10', NULL, 6.00, NULL, 'TOTAL_TOP', 7, 1, NOW(), NOW()),

-- Total Bottom (Lẻ + Tổng 11-17)
('sicbo_parity_odd', 'Lẻ', NULL, 0.97, NULL, 'TOTAL_BOTTOM', 0, 1, NOW(), NOW()),
('sicbo_total_11', 'Tổng 11', NULL, 6.00, NULL, 'TOTAL_BOTTOM', 1, 1, NOW(), NOW()),
('sicbo_total_12', 'Tổng 12', NULL, 6.00, NULL, 'TOTAL_BOTTOM', 2, 1, NOW(), NOW()),
('sicbo_total_13', 'Tổng 13', NULL, 8.00, NULL, 'TOTAL_BOTTOM', 3, 1, NOW(), NOW()),
('sicbo_total_14', 'Tổng 14', NULL, 12.00, NULL, 'TOTAL_BOTTOM', 4, 1, NOW(), NOW()),
('sicbo_total_15', 'Tổng 15', NULL, 14.00, NULL, 'TOTAL_BOTTOM', 5, 1, NOW(), NOW()),
('sicbo_total_16', 'Tổng 16', NULL, 18.00, NULL, 'TOTAL_BOTTOM', 6, 1, NOW(), NOW()),
('sicbo_total_17', 'Tổng 17', NULL, 30.00, NULL, 'TOTAL_BOTTOM', 7, 1, NOW(), NOW()),

-- Single bets (Một mặt)
('sicbo_single_1', 'Một mặt 1', NULL, 0.97, NULL, 'SINGLE', 0, 1, NOW(), NOW()),
('sicbo_single_2', 'Một mặt 2', NULL, 0.97, NULL, 'SINGLE', 1, 1, NOW(), NOW()),
('sicbo_single_3', 'Một mặt 3', NULL, 0.97, NULL, 'SINGLE', 2, 1, NOW(), NOW()),
('sicbo_single_4', 'Một mặt 4', NULL, 0.97, NULL, 'SINGLE', 3, 1, NOW(), NOW()),
('sicbo_single_5', 'Một mặt 5', NULL, 0.97, NULL, 'SINGLE', 4, 1, NOW(), NOW()),
('sicbo_single_6', 'Một mặt 6', NULL, 0.97, NULL, 'SINGLE', 5, 1, NOW(), NOW()),

-- Dice pair bets (15 combinations)
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
('sicbo_pair_5_6', 'Cặp 5-6', 'Cược cặp xúc xắc 5 và 6', 5.00, NULL, 'DICE_PAIR', 14, 1, NOW(), NOW()),

-- Dice pair double bets (6 combinations)
('sicbo_pair_double_1', 'Cặp đôi 1-1', 'Cược cặp đôi 1-1', 8.00, NULL, 'DICE_PAIR_DOUBLE', 0, 1, NOW(), NOW()),
('sicbo_pair_double_2', 'Cặp đôi 2-2', 'Cược cặp đôi 2-2', 8.00, NULL, 'DICE_PAIR_DOUBLE', 1, 1, NOW(), NOW()),
('sicbo_pair_double_3', 'Cặp đôi 3-3', 'Cược cặp đôi 3-3', 8.00, NULL, 'DICE_PAIR_DOUBLE', 2, 1, NOW(), NOW()),
('sicbo_pair_double_4', 'Cặp đôi 4-4', 'Cược cặp đôi 4-4', 8.00, NULL, 'DICE_PAIR_DOUBLE', 3, 1, NOW(), NOW()),
('sicbo_pair_double_5', 'Cặp đôi 5-5', 'Cược cặp đôi 5-5', 8.00, NULL, 'DICE_PAIR_DOUBLE', 4, 1, NOW(), NOW()),
('sicbo_pair_double_6', 'Cặp đôi 6-6', 'Cược cặp đôi 6-6', 8.00, NULL, 'DICE_PAIR_DOUBLE', 5, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE code = code;

-- =====================================================
-- 8. XOC DIA QUICK BET CONFIGS
-- =====================================================
-- Tạo cấu hình quick bet cho game Xóc Đĩa

INSERT INTO xoc_dia_quick_bet_config (
    code,
    name,
    description,
    payout_multiplier,
    fee_rate,
    pattern,
    layout_group,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
-- TOP group
('chan', 'Chẵn', NULL, 1.96, NULL, NULL, 'TOP', 1, 1, NOW(), NOW()),
('tai', 'Tài', NULL, 1.95, NULL, NULL, 'TOP', 2, 1, NOW(), NOW()),
('xiu', 'Xỉu', NULL, 1.95, NULL, NULL, 'TOP', 3, 1, NOW(), NOW()),
('le', 'Lẻ', NULL, 1.96, NULL, NULL, 'TOP', 4, 1, NOW(), NOW()),
('two-two', '2 Trắng 2 Đỏ', NULL, 2.55, NULL, 'white,white,red,red', 'TOP', 5, 1, NOW(), NOW()),

-- BOTTOM group
('four-white', '4 Trắng', NULL, 14.50, NULL, 'white,white,white,white', 'BOTTOM', 1, 1, NOW(), NOW()),
('three-white-one-red', '3 Trắng 1 Đỏ', NULL, 1.95, NULL, 'white,white,white,red', 'BOTTOM', 2, 1, NOW(), NOW()),
('three-red-one-white', '3 Đỏ 1 Trắng', NULL, 1.95, NULL, 'red,red,red,white', 'BOTTOM', 3, 1, NOW(), NOW()),
('four-red', '4 Đỏ', NULL, 14.50, NULL, 'red,red,red,red', 'BOTTOM', 4, 1, NOW(), NOW()),
('four-white-or-four-red', '4 Trắng & 4 Đỏ', NULL, 7.00, NULL, 'white,white,white,white,red,red,red,red', 'BOTTOM', 5, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE code = code;

-- =====================================================
-- 9. MARQUEE NOTIFICATION
-- =====================================================
-- Tạo thông báo marquee mẫu

INSERT INTO marquee_notifications (
    content,
    is_active,
    display_order,
    text_color,
    background_color,
    font_size,
    speed,
    created_at,
    created_by
) VALUES (
    '🎉 CHÀO MỪNG ĐẾN VỚI LOTO79 - NỀN TẢNG CÁ CƯỢC HÀNG ĐẦU VIỆT NAM! 🥳 TẶNG NGAY 100% TIỀN NẠP LẦN ĐẦU + 50 FREE SPIN! 🎰 ĐĂNG KÝ NGAY ĐỂ NHẬN ƯU ĐÃI ĐẶC BIỆT! 💰',
    true,
    1,
    '#FF0000',
    '#FFFFFF',
    16,
    50,
    NOW(),
    'admin'
) ON DUPLICATE KEY UPDATE id = id;

-- =====================================================
-- HOÀN TẤT
-- =====================================================

SET FOREIGN_KEY_CHECKS = 1;

-- Kiểm tra dữ liệu đã được tạo
SELECT '=== KIỂM TRA DỮ LIỆU ĐÃ TẠO ===' AS status;
SELECT COUNT(*) AS total_users FROM users;
SELECT COUNT(*) AS total_payment_methods FROM payment_methods;
SELECT COUNT(*) AS total_system_settings FROM system_settings;
SELECT COUNT(*) AS total_betting_odds FROM betting_odds;
SELECT COUNT(*) AS total_telegram_configs FROM telegram_config;
SELECT COUNT(*) AS total_stream_configs FROM stream_configs;
SELECT COUNT(*) AS total_sicbo_quick_bet_configs FROM sicbo_quick_bet_config;
SELECT COUNT(*) AS total_xoc_dia_quick_bet_configs FROM xoc_dia_quick_bet_config;
SELECT COUNT(*) AS total_marquee_notifications FROM marquee_notifications;

SELECT '✅ Script khởi tạo dữ liệu ban đầu đã hoàn tất!' AS status;


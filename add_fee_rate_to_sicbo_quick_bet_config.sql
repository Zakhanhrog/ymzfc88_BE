-- Thêm cột fee_rate vào bảng sicbo_quick_bet_config
-- Tỷ lệ phế cho bàn 1 (ví dụ 0.03 nghĩa là 3%)
-- NULL hoặc 0 nghĩa là không tính phế cho loại đánh này
ALTER TABLE sicbo_quick_bet_config 
ADD COLUMN fee_rate DECIMAL(5, 4) NULL DEFAULT NULL;

-- Cập nhật comment cho cột
COMMENT ON COLUMN sicbo_quick_bet_config.fee_rate IS 'Tỷ lệ phế cho bàn 1 (ví dụ 0.03 nghĩa là 3%). NULL hoặc 0 nghĩa là không tính phế cho loại đánh này';


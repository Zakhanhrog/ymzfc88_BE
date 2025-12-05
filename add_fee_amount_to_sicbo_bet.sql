-- Thêm cột fee_amount vào bảng sicbo_bet
-- Lưu số tiền phế khi đặt cược (chỉ áp dụng cho bàn 1)
ALTER TABLE sicbo_bet 
ADD COLUMN fee_amount DECIMAL(18, 2) NULL DEFAULT NULL;

-- Cập nhật comment cho cột
COMMENT ON COLUMN sicbo_bet.fee_amount IS 'Số tiền phế (chỉ áp dụng cho bàn 1). NULL hoặc 0 nghĩa là không có phế';


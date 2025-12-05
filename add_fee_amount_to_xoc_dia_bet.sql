-- Thêm cột fee_amount vào bảng xoc_dia_bet
-- Lưu số tiền phế khi đặt cược
ALTER TABLE xoc_dia_bet 
ADD COLUMN fee_amount DECIMAL(18, 2) NULL DEFAULT NULL;

-- Cập nhật comment cho cột
COMMENT ON COLUMN xoc_dia_bet.fee_amount IS 'Số tiền phế. NULL hoặc 0 nghĩa là không có phế';


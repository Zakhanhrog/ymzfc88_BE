-- Thêm cột bao_amount vào bảng sicbo_bet
-- Lưu số tiền bão (chỉ áp dụng cho bàn 2)
-- Tiền thua trong trường hợp đặc biệt:
-- - Ra bộ ba nhỏ (111,222,333) mà đánh Tài → thua
-- - Ra bộ ba lớn (444,555,666) mà đánh Xỉu → thua
ALTER TABLE sicbo_bet 
ADD COLUMN bao_amount DECIMAL(18, 2) NULL DEFAULT NULL;

-- Cập nhật comment cho cột
COMMENT ON COLUMN sicbo_bet.bao_amount IS 'Số tiền bão (chỉ áp dụng cho bàn 2). Tiền thua trong trường hợp đặc biệt khi ra bộ ba';


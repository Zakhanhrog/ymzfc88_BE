-- =====================================================
-- SCRIPT XÓA STREAM CONFIGS TRÙNG LẶP
-- =====================================================
-- Script này xóa các stream config trùng lặp, chỉ giữ lại 1 bản ghi cho mỗi cấu hình
-- 
-- CÁCH SỬ DỤNG:
-- Chạy script này để xóa dữ liệu trùng lặp trước khi chạy backend
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Xóa các XocDia stream config trùng lặp (chỉ giữ lại bản ghi đầu tiên)
DELETE t1 FROM stream_configs t1
INNER JOIN stream_configs t2 
WHERE t1.id > t2.id 
  AND t1.game_type = 'XOC_DIA' 
  AND t1.table_number IS NULL
  AND t2.game_type = 'XOC_DIA' 
  AND t2.table_number IS NULL;

-- Xóa các Sicbo stream config trùng lặp cho table 1 (chỉ giữ lại bản ghi đầu tiên)
DELETE t1 FROM stream_configs t1
INNER JOIN stream_configs t2 
WHERE t1.id > t2.id 
  AND t1.game_type = 'SICBO' 
  AND t1.table_number = 1
  AND t2.game_type = 'SICBO' 
  AND t2.table_number = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- Kiểm tra kết quả
SELECT '=== KIỂM TRA STREAM CONFIGS SAU KHI XÓA ===' AS status;
SELECT game_type, table_number, COUNT(*) AS count 
FROM stream_configs 
GROUP BY game_type, table_number;

SELECT '✅ Đã xóa stream configs trùng lặp!' AS status;


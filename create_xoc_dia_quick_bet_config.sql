-- Script tạo bảng cấu hình quick bet cho game Xóc Đĩa
-- Chạy script này một lần sau khi deploy backend

CREATE TABLE IF NOT EXISTS xoc_dia_quick_bet_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    payout_multiplier DECIMAL(10,2) NOT NULL,
    pattern VARCHAR(200),
    layout_group VARCHAR(50) NOT NULL DEFAULT 'TOP',
    display_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_xoc_dia_quick_bet_display ON xoc_dia_quick_bet_config(display_order);
CREATE INDEX idx_xoc_dia_quick_bet_active ON xoc_dia_quick_bet_config(is_active, display_order);

-- Dữ liệu mẫu ban đầu khớp với giao diện hiện tại
INSERT INTO xoc_dia_quick_bet_config (code, name, payout_multiplier, pattern, layout_group, display_order)
VALUES
    ('even', 'Chẵn', 1.96, NULL, 'TOP', 1),
    ('two-two', '2 Trắng 2 Đỏ', 2.55, 'white,white,red,red', 'TOP', 2),
    ('odd', 'Lẻ', 1.96, NULL, 'TOP', 3),
    ('four-white', '4 Trắng', 14.5, 'white,white,white,white', 'BOTTOM', 4),
    ('three-white', 'Lớn', 3.7, NULL, 'BOTTOM', 5),
    ('three-red', 'Nhỏ', 3.7, NULL, 'BOTTOM', 6),
    ('four-red', '4 Đỏ', 14.5, 'red,red,red,red', 'BOTTOM', 7)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    payout_multiplier = VALUES(payout_multiplier),
    pattern = VALUES(pattern),
    layout_group = VALUES(layout_group),
    display_order = VALUES(display_order),
    is_active = TRUE;

INSERT INTO xoc_dia_quick_bet_config (code, name, payout_multiplier, pattern, layout_group, display_order)
VALUES ('four-white-or-four-red', '4 Trắng & 4 Đỏ', 7.00, 'white,white,white,white,red,red,red,red', 'BOTTOM', 8)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    payout_multiplier = VALUES(payout_multiplier),
    pattern = VALUES(pattern),
    layout_group = VALUES(layout_group),
    display_order = VALUES(display_order),
    is_active = TRUE;

SELECT * FROM xoc_dia_quick_bet_config;



-- Migration script để thêm chức năng điểm thưởng
-- Chạy file này để cập nhật database

-- 1. Thêm cột points vào bảng users
ALTER TABLE users ADD COLUMN points BIGINT NOT NULL DEFAULT 0 AFTER balance;

-- 2. Tạo bảng user_points
CREATE TABLE user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    total_points DECIMAL(10,0) NOT NULL DEFAULT 0,
    lifetime_earned DECIMAL(10,0) NOT NULL DEFAULT 0,
    lifetime_spent DECIMAL(10,0) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_points_user_id (user_id)
);

-- 3. Tạo bảng point_transactions
CREATE TABLE point_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    transaction_code VARCHAR(20) NOT NULL UNIQUE,
    type ENUM('EARN', 'SPEND', 'ADMIN_ADD', 'ADMIN_SUBTRACT', 'DEPOSIT_BONUS', 'REFUND') NOT NULL,
    points DECIMAL(10,0) NOT NULL,
    balance_before DECIMAL(10,0) NOT NULL,
    balance_after DECIMAL(10,0) NOT NULL,
    description VARCHAR(500),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_point_transactions_user_id (user_id),
    INDEX idx_point_transactions_created_at (created_at),
    INDEX idx_point_transactions_type (type),
    INDEX idx_point_transactions_reference (reference_type, reference_id)
);

-- 4. Tạo trigger để tự động tạo user_points khi tạo user mới
DELIMITER //
CREATE TRIGGER after_user_insert
    AFTER INSERT ON users
    FOR EACH ROW
BEGIN
    INSERT INTO user_points (user_id, total_points, lifetime_earned, lifetime_spent)
    VALUES (NEW.id, 0, 0, 0);
END//
DELIMITER ;

-- 5. Khởi tạo user_points cho các user hiện có (nếu có)
INSERT INTO user_points (user_id, total_points, lifetime_earned, lifetime_spent)
SELECT id, COALESCE(points, 0), 0, 0
FROM users
WHERE id NOT IN (SELECT user_id FROM user_points);

-- 6. Cập nhật points trong bảng users từ user_points (đồng bộ dữ liệu)
UPDATE users u 
JOIN user_points up ON u.id = up.user_id 
SET u.points = up.total_points;

-- Kiểm tra dữ liệu
SELECT 'Migration completed successfully' as status;

-- Hiển thị thống kê
SELECT 
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM user_points) as total_user_points,
    (SELECT COUNT(*) FROM point_transactions) as total_point_transactions;
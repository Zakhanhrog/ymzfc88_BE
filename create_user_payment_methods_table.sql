-- Migration script for user_payment_methods table
-- Run this script to create the table for storing user payment methods

CREATE TABLE user_payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type ENUM('MOMO', 'BANK', 'USDT', 'ZALO_PAY', 'VIET_QR') NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    bank_code VARCHAR(20),
    note TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_user_payment_methods_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_user_payment_methods_user_id (user_id),
    INDEX idx_user_payment_methods_type (type),
    INDEX idx_user_payment_methods_default (user_id, is_default),
    
    -- Unique constraint to prevent duplicate account numbers for same user and type
    UNIQUE KEY uk_user_payment_methods_account (user_id, account_number, type)
);

-- Sample data (optional - remove if not needed)
-- INSERT INTO user_payment_methods (user_id, name, type, account_number, account_name, bank_code, is_default, is_verified) 
-- VALUES 
-- (1, 'Tài khoản VCB chính', 'BANK', '1234567890', 'Nguyen Van A', 'VCB', TRUE, TRUE),
-- (1, 'Ví MoMo cá nhân', 'MOMO', '0901234567', 'Nguyen Van A', NULL, FALSE, TRUE);
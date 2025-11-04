-- Run this SQL script to create telegram_config table and insert default data
-- Make sure to run this before starting the backend

-- Create table
CREATE TABLE IF NOT EXISTS telegram_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bot_token VARCHAR(100) NOT NULL,
    chat_id VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default config from application.properties
INSERT INTO telegram_config (bot_token, chat_id, enabled, description, created_at, updated_at) 
VALUES (
    '8421520125:AAGWiE88hE5q8yRwGTtP2cjbMGiqdiRf_SA',
    '-4906012064', 
    true, 
    'Default Telegram config migrated from application.properties',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE 
    bot_token = VALUES(bot_token),
    chat_id = VALUES(chat_id),
    enabled = VALUES(enabled),
    description = VALUES(description),
    updated_at = NOW();

-- Verify the data
SELECT * FROM telegram_config;

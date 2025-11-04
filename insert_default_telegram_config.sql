-- Insert default Telegram config from application.properties
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

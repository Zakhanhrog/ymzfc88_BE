-- Script để tạo thông báo mẫu
-- Chạy script này sau khi backend đã khởi động

INSERT INTO marquee_notifications (
    content, 
    is_active, 
    display_order, 
    text_color, 
    background_color, 
    font_size, 
    speed, 
    created_at, 
    created_by
) VALUES (
    '🎉 CHÀO MỪNG ĐẾN VỚI LOTO79 - NỀN TẢNG CÁ CƯỢC HÀNG ĐẦU VIỆT NAM! 🥳 TẶNG NGAY 100% TIỀN NẠP LẦN ĐẦU + 50 FREE SPIN! 🎰 ĐĂNG KÝ NGAY ĐỂ NHẬN ƯU ĐÃI ĐẶC BIỆT! 💰',
    true,
    1,
    '#FF0000',
    '#FFFFFF',
    16,
    50,
    NOW(),
    'admin'
);

-- Kiểm tra dữ liệu đã insert
SELECT * FROM marquee_notifications;

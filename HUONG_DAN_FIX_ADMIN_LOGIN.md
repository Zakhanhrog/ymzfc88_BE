# HƯỚNG DẪN FIX LỖI ĐĂNG NHẬP ADMIN

## Vấn đề:
- Không thể đăng nhập admin với lỗi 400 Bad Request
- Nguyên nhân: BCrypt hash trong SQL script không chính xác hoặc thiếu C2 password

## Giải pháp:

### Cách 1: Reset và để backend tự tạo (KHUYẾN NGHỊ)

1. **Xóa admin user cũ:**
   ```bash
   mysql -u [username] -p [database_name] < reset_admin_user.sql
   ```

2. **Chạy backend:**
   ```bash
   cd ymzfc88_BE
   mvn spring-boot:run
   ```

3. **Backend sẽ tự động:**
   - Tạo admin user mới với password hash chính xác
   - Tạo C2 password (mật khẩu bảo vệ cấp 2)
   - Tạo user_wallet cho admin

4. **Đăng nhập với:**
   - Username: `admin`
   - Password: `admin123`
   - C2 Password: `admin123`

### Cách 2: Cập nhật C2 password cho admin user hiện có

Nếu admin user đã tồn tại nhưng thiếu C2 password:

1. **Chạy backend** - DataInitializer sẽ tự động thêm C2 password cho admin user hiện có

2. **Hoặc chạy SQL thủ công** (không khuyến nghị vì hash có thể không chính xác):
   ```sql
   -- Chỉ dùng nếu chắc chắn về BCrypt hash
   UPDATE users 
   SET c2_password_hash = '[BCRYPT_HASH]',
       c2_password_updated_at = NOW()
   WHERE username = 'admin';
   ```

## Lưu ý:

- **DataInitializer đã được cập nhật** để tự động tạo C2 password cho admin user
- Nếu admin user đã tồn tại nhưng thiếu C2 password, DataInitializer sẽ tự động thêm
- C2 password mặc định giống với password chính: `admin123`

## Kiểm tra:

Sau khi chạy backend, kiểm tra log:
```
Default admin user created: admin with C2 password
```
hoặc
```
C2 password added to existing admin user: admin
```

Sau đó thử đăng nhập lại với:
- Username: `admin`
- Password: `admin123`
- C2 Password: `admin123`


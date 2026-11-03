# Hướng dẫn cấu hình Email (SMTP) cho F6S Store

Tài liệu này hướng dẫn chi tiết từng bước thiết lập dịch vụ gửi email cấp tài khoản và thông báo cho hệ thống F6S Store.

---

## 1. Cơ chế cấu hình Email trong hệ thống

Hệ thống hỗ trợ đọc cấu hình SMTP linh hoạt từ hai nguồn:
1. **File cấu hình cục bộ `application-local.properties`** (khuyến nghị khi chạy trên IntelliJ / máy phát triển):
   - Nằm tại thư mục gốc của dự án (`DuAnTNDEMO/DemoDATN/application-local.properties`).
   - Đã được đưa vào `.gitignore` để tránh vô tình commit mật khẩu ứng dụng lên kho mã nguồn.
   - Tham khảo file mẫu `application-local.properties.example`.
2. **Biến môi trường hệ điều hành (Environment Variables)** (khuyến nghị cho môi trường CI/CD, Docker, máy chủ sản xuất):
   - `MAIL_HOST`: Địa chỉ máy chủ SMTP (ví dụ: `smtp.gmail.com`).
   - `MAIL_PORT`: Cổng kết nối (ví dụ: `587`).
   - `MAIL_USERNAME`: Tên tài khoản / địa chỉ email gửi.
   - `MAIL_PASSWORD`: Mật khẩu ứng dụng (App Password) hoặc mật khẩu SMTP.
   - `MAIL_FROM`: Địa chỉ người gửi hiển thị (nếu để trống sẽ tự động lấy `MAIL_USERNAME`).
   - `MAIL_AUTH`: `true` (mặc định).
   - `MAIL_STARTTLS`: `true` (mặc định).

Khi ứng dụng khởi động, hệ thống sẽ tự động kiểm tra và ghi đúng một dòng log rõ ràng:
- Khi đã cấu hình: `Email: ĐÃ cấu hình (<host>:<port>)`
- Khi chưa cấu hình: `Email: CHƯA cấu hình, tài khoản mới sẽ không được gửi mail`
*(Lưu ý: Hệ thống TUYỆT ĐỐI không ghi mật khẩu hay thông tin bí mật ra log).*

---

## 2. Hướng dẫn thiết lập Gmail (Gửi thư thật)

Để gửi email thật qua máy chủ SMTP của Google (`smtp.gmail.com`), Google bắt buộc sử dụng **Mật khẩu ứng dụng (App Password)** thay vì mật khẩu đăng nhập thông thường.

### Bước 1: Bật Xác minh 2 bước (2-Step Verification) cho tài khoản Google
1. Đăng nhập vào tài khoản Google của bạn tại: [https://myaccount.google.com/](https://myaccount.google.com/)
2. Ở menu bên trái, chọn **Bảo mật** (Security).
3. Trong mục **Cách bạn đăng nhập vào Google** (How you sign in to Google), tìm và chọn **Xác minh 2 bước** (2-Step Verification).
4. Làm theo hướng dẫn trên màn hình để kích hoạt (nếu tài khoản đã bật Xác minh 2 bước thì chuyển sang Bước 2).

### Bước 2: Tạo "Mật khẩu ứng dụng" (App Password)
1. Truy cập trực tiếp trang Mật khẩu ứng dụng: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)  
   *(Hoặc tìm kiếm từ khóa "App passwords" trong ô tìm kiếm của Google Account).*
2. Nhập tên ứng dụng, ví dụ: `F6S Store Local` hoặc `DemoDATN`.
3. Nhấn **Tạo** (Create).
4. Google sẽ hiển thị một chuỗi **16 ký tự** màu vàng (ví dụ: `abcd efgh ijkl mnop`).
5. **Sao chép chuỗi 16 ký tự này** (chuỗi này chỉ hiện một lần duy nhất). Không cần khoảng trắng khi dán vào cấu hình.

### Bước 3: Điền vào `application-local.properties`
Mở file `application-local.properties` (tạo mới nếu chưa có, copy từ `application-local.properties.example`) và thêm các dòng sau:

```properties
# Cấu hình SMTP Gmail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=dia_chi_email_cua_ban@gmail.com
spring.mail.password=abcdefghijklmnop
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
shop.mail.from=dia_chi_email_cua_ban@gmail.com
```

### Bước 4: Kiểm tra kết nối bằng nút "Gửi email thử"
1. Khởi động ứng dụng.
2. Đăng nhập tài khoản Quản trị viên (Admin).
3. Vào trang **Hồ sơ của tôi** (`/admin/account`).
4. Tìm khối **Cấu hình & Thử nghiệm Email**, nhấn nút **Gửi email thử**.
5. Hệ thống sẽ gửi một email thử nghiệm đến địa chỉ email của bạn và thông báo kết quả tức thì trên màn hình.

---

## 3. Hướng dẫn thử nghiệm bằng máy chủ SMTP giả lập (MailHog / smtp4dev)

Nếu bạn không muốn dùng Gmail thật hoặc làm việc trong môi trường offline/cục bộ, hãy sử dụng **MailHog** hoặc **smtp4dev**. Các công cụ này chạy một máy chủ SMTP nội bộ, bắt tất cả email gửi đi và hiển thị trên giao diện web trực quan mà không gửi ra ngoài Internet.

### Lựa chọn A: Dùng MailHog (Khuyến nghị)
1. Tải MailHog:
   - Dùng Docker:
     ```bash
     docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
     ```
   - Hoặc tải file `.exe` trực tiếp từ GitHub Releases của MailHog: [https://github.com/mailhog/MailHog/releases](https://github.com/mailhog/MailHog/releases) và chạy file `MailHog_windows_amd64.exe`.
2. MailHog mở 2 cổng:
   - Cổng SMTP: `1025` (dùng để nhận thư).
   - Cổng Giao diện Web: `http://localhost:8025` (mở trên trình duyệt để đọc thư đến).
3. Cấu hình trong `application-local.properties`:
   ```properties
   spring.mail.host=127.0.0.1
   spring.mail.port=1025
   spring.mail.username=
   spring.mail.password=
   spring.mail.properties.mail.smtp.auth=false
   spring.mail.properties.mail.smtp.starttls.enable=false
   spring.mail.properties.mail.smtp.connectiontimeout=10000
   spring.mail.properties.mail.smtp.timeout=10000
   spring.mail.properties.mail.smtp.writetimeout=10000
   shop.mail.from=noreply@f6s-store.test
   ```

### Lựa chọn B: Dùng smtp4dev
1. Cài đặt hoặc chạy bằng Docker:
   ```bash
   docker run -d -p 2525:25 -p 5000:80 rnwood/smtp4dev
   ```
2. Mở giao diện xem thư tại `http://localhost:5000`.
3. Cấu hình trong `application-local.properties`:
   ```properties
   spring.mail.host=127.0.0.1
   spring.mail.port=2525
   spring.mail.username=
   spring.mail.password=
   spring.mail.properties.mail.smtp.auth=false
   spring.mail.properties.mail.smtp.starttls.enable=false
   spring.mail.properties.mail.smtp.connectiontimeout=10000
   spring.mail.properties.mail.smtp.timeout=10000
   spring.mail.properties.mail.smtp.writetimeout=10000
   shop.mail.from=noreply@f6s-store.test
   ```

---

## 4. Xử lý các lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách khắc phục |
|---|---|---|
| Màn hình báo "Chưa cấu hình thông tin máy chủ email..." | `spring.mail.host` đang để trống | Điền cấu hình vào `application-local.properties` và khởi động lại ứng dụng. |
| Màn hình báo "Xác thực SMTP thất bại..." | Sai tài khoản hoặc mật khẩu thông thường thay vì App Password | Kiểm tra lại email Gmail và đảm bảo tạo App Password 16 ký tự từ mục Bảo mật Google. |
| Màn hình báo "Không thể kết nối tới máy chủ SMTP..." | Sai cổng, sai host hoặc firewall/mạng chặn | Kiểm tra kết nối mạng, đảm bảo host `smtp.gmail.com` cổng `587` không bị chặn bởi VPN hoặc phần mềm diệt virus. |
| Tài khoản mới tạo xong nhưng có cảnh báo "Chưa gửi được email" | Email bị lỗi khi gửi | Tài khoản vẫn được lưu trong cơ sở dữ liệu. Sau khi sửa cấu hình email, nhấn nút **Gửi lại tài khoản** trên màn hình chi tiết để gửi lại mật khẩu tạm. |

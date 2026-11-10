# Hướng dẫn cấu hình Email (SMTP) cho F6 Shop

Tài liệu này hướng dẫn chi tiết cách cấu hình dịch vụ gửi email cấp tài khoản, xác nhận đơn hàng và thông báo trạng thái đơn hàng cho hệ thống F6 Shop.

---

## 1. Lưu ý quan trọng & Các thành phần đã gỡ bỏ

Theo kiến trúc chuẩn và bảo mật của Spring Boot:
- **ĐÃ GỠ BỎ**: Toàn bộ form nhập cấu hình SMTP (host, port, tài khoản, mật khẩu, From) trên giao diện web `/admin/account`.
- **ĐÃ GỠ BỎ**: Cơ chế nạp nóng `DynamicJavaMailSender` và mọi mã tự động ghi/sửa file `application-local.properties` lúc ứng dụng đang chạy.
- **CƠ CHẾ CHUẨN HIỆN TẠI**: Cấu hình email chỉ được đọc tĩnh lúc ứng dụng khởi động từ file cấu hình bảo mật `application-local.properties` (được gitignore) hoặc thông qua biến môi trường hệ thống. Khi thay đổi cấu hình, bạn chỉ cần khởi động lại ứng dụng.
- **GIỮ NGUYÊN**: Dòng trạng thái CHỈ ĐỌC tại `/admin/account` hiển thị thông tin máy chủ và tài khoản đã che mờ; nút "Gửi email thử"; nút "Gửi lại tài khoản" tại chi tiết khách hàng và nhân viên.

---

## 2. Vị trí đặt file & Nội dung mẫu cấu hình

### Vị trí file
Tạo hoặc chỉnh sửa file `application-local.properties` nằm ngay tại thư mục gốc của project backend:
`DuAnTNDEMO/DemoDATN/application-local.properties`

*(File này đã được khai báo trong `.gitignore` để tuyệt đối không bị commit mật khẩu ứng dụng lên kho mã nguồn Git).*

### Nội dung mẫu cho Gmail (gửi thư thật):
```properties
# ========================================================
# CẤU HÌNH GỬI EMAIL CHUẨN SPRING BOOT (GMAIL SMTP)
# ========================================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=dia_chi_email_cua_ban@gmail.com
spring.mail.password=abcdefghijklmnop

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000

# Thông tin cửa hàng hiển thị trong thư
shop.name=F6 Shop
shop.mail.from=dia_chi_email_cua_ban@gmail.com
shop.address=123 Đường Cầu Giấy, Phường Quan Hoa, Quận Cầu Giấy, Hà Nội
shop.hotline=1900 1234
shop.support-email=support@f6shop.com
shop.public-url=http://localhost:8080
```

> **Lưu ý triển khai**: Khi đưa hệ thống lên máy chủ thực tế (Production), cần đổi `shop.public-url` sang domain thật (ví dụ `https://f6shop.com`) để các liên kết "Đăng nhập", "Xem chi tiết đơn hàng" trong thư hoạt động chính xác.

---

## 3. Các bước lấy Mật khẩu ứng dụng Gmail (16 ký tự)

Để gửi email thông qua máy chủ SMTP của Google (`smtp.gmail.com`), bạn không thể dùng mật khẩu đăng nhập cá nhân thông thường mà bắt buộc phải tạo **Mật khẩu ứng dụng (App Password)** gồm 16 ký tự:

### Bước 1: Bật Xác minh 2 bước (2-Step Verification)
1. Đăng nhập vào tài khoản Google: [https://myaccount.google.com/](https://myaccount.google.com/)
2. Chọn menu **Bảo mật** (Security) ở góc bên trái.
3. Trong mục **Cách bạn đăng nhập vào Google**, tìm chọn **Xác minh 2 bước** (2-Step Verification).
4. Thực hiện theo hướng dẫn trên màn hình để bật xác minh (qua số điện thoại hoặc Google Prompt).

### Bước 2: Tạo Mật khẩu ứng dụng
1. Truy cập trực tiếp đường link: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
2. Nhập tên ứng dụng nhận diện (ví dụ: `F6 Shop Local`).
3. Bấm nút **Tạo** (Create).
4. Google sẽ sinh ra một chuỗi **16 ký tự chữ cái** (ví dụ: `xxxx yyyy zzzz wwww`).
5. Sao chép 16 ký tự này và dán vào thuộc tính `spring.mail.password` trong file `application-local.properties` (loại bỏ khoảng trắng).

### Bước 3: Khởi động lại ứng dụng & Kiểm tra
1. Khởi động lại Spring Boot backend (`DemoDATN`).
2. Mở trình duyệt, đăng nhập tài khoản Quản trị viên (Admin).
3. Vào trang **Hồ sơ của tôi** (`/admin/account`).
4. Xem dòng trạng thái: `Email: Đã cấu hình (smtp.gmail.com:587, dia_***@gmail.com)`.
5. Nhập email của bạn vào ô "Người nhận email thử" và bấm **Gửi email thử** để kiểm tra hoạt động SMTP.

---

## 4. Thử nghiệm bằng máy chủ SMTP giả lập (MailHog)

Nếu bạn đang làm việc offline hoặc không muốn gửi email thật đến hộp thư cá nhân, hãy sử dụng **MailHog**. MailHog giả lập một máy chủ SMTP nội bộ, thu thập mọi thư gửi đi và hiển thị giao diện Web UI trực quan.

### Bước 1: Chạy MailHog
- Chạy bằng Docker:
  ```bash
  docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
  ```
- Hoặc tải file `.exe` từ [MailHog Releases](https://github.com/mailhog/MailHog/releases) và khởi chạy trên Windows.

### Bước 2: Cấu hình `application-local.properties` cho MailHog
```properties
spring.mail.host=127.0.0.1
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.starttls.required=false
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000

shop.name=F6 Shop
shop.mail.from=noreply@f6shop.test
shop.public-url=http://localhost:8080
```

### Bước 3: Xem thư gửi đến
Mở trình duyệt truy cập: `http://localhost:8025` để xem toàn bộ danh sách email được gửi từ hệ thống.

---

## 5. Bảng xử lý sự cố thường gặp

| Trường hợp | Nguyên nhân | Hướng xử lý |
|---|---|---|
| Màn hình báo "Email: Chưa cấu hình" | `spring.mail.host` đang để trống | Điền cấu hình vào `application-local.properties` và khởi động lại server. |
| Báo lỗi: "Xác thực SMTP thất bại..." | Sai username hoặc dùng mật khẩu thông thường thay vì App Password | Kiểm tra lại địa chỉ Gmail và tạo lại Mật khẩu ứng dụng 16 ký tự từ trang bảo mật Google. |
| Báo lỗi: "Không thể kết nối tới máy chủ SMTP..." | Sai cổng kết nối, sai host hoặc mạng/firewall chặn cổng 587 | Kiểm tra kết nối Internet, kiểm tra host `smtp.gmail.com` và port `587`. |
| Tạo khách hàng/nhân viên hiện cảnh báo vàng "Chưa gửi được email" | Máy chủ SMTP từ chối gửi hoặc chưa cấu hình mail | Tài khoản vẫn được lưu an toàn trong DB. Sau khi kiểm tra cấu hình SMTP, nhấn nút **Gửi lại tài khoản** trong chi tiết hồ sơ để gửi email. Khi gửi lại thành công, cảnh báo sẽ tự động biến mất. |

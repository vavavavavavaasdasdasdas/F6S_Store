# Hướng dẫn Kiểm thử Cổng Thanh toán VNPay (F6S Store)

Dự án F6S Store tích hợp cổng thanh toán VNPay chuẩn API `2.1.0` sử dụng thuật toán bảo mật `HMAC-SHA512`.

---

## 1. Cấu hình Cổng VNPay Sandbox
Mặc định trong `application.properties`:
- **Terminal ID (vnp_TmnCode)**: `L5HQXLHO`
- **Secret Key (vnp_HashSecret)**: `ETHX3ND34R4DBRK5V8J95H6I8G5QO040`
- **URL Cổng Sandbox**: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`
- **Reconcile on Return**: `true` (Tự động cập nhật `PAID` và ghi nhận mã giao dịch ngay khi khách hàng hoàn tất thanh toán và trình duyệt chuyển hướng về trang web)

---

## 2. Thông tin Thẻ Test VNPay Sandbox

Khi thanh toán trên cổng VNPay Sandbox, chọn **Thẻ ATM và tài khoản ngân hàng** -> chọn ngân hàng **NCB**:

| Thuộc tính | Giá trị test |
| :--- | :--- |
| **Ngân hàng** | **NCB** (Ngân hàng Quốc Dân) |
| **Số thẻ** | `9704198526191432198` |
| **Tên chủ thẻ** | `NGUYEN VAN A` |
| **Ngày phát hành** | `07/15` |
| **Mật khẩu OTP** | `123456` |

---

## 3. Chế độ Local Checkout Simulator (Trình diễn trực tiếp không cần mạng)
Nếu muốn kiểm thử giao diện thanh toán mô phỏng nội bộ ngay trên localhost:
Đặt trong `application-local.properties` hoặc biến môi trường:
```properties
payment.vnpay.local-checkout-enabled=true
```
Hệ thống sẽ mở giao diện giả lập VNPay tích hợp sẵn tại `/api/shop/payments/vnpay/checkout` với đầy đủ tính năng:
- Chọn phương thức: **VNPAY-QR**, **Thẻ ATM**, **Thẻ quốc tế**
- Tự động điền thẻ test và xác thực OTP
- Kiểm tra chữ ký số HMAC-SHA512 trước khi cập nhật đơn hàng thành công

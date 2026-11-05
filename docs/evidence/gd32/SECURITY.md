# Kết quả bảo mật — 8 kịch bản

| Mã / kịch bản | Status thực tế | Mong đợi | Kết quả |
|---|---|---|---|
| SEC01 khách vào quản lý sản phẩm | 302 | 302 | PASS |
| SEC02 khách vào checkout | 302 | 302 | PASS |
| SEC03 khách đọc inbox | 403 | 403 | PASS |
| SEC04 thêm giỏ thiếu CSRF | 403 | 403 | PASS |
| SEC05 thêm giỏ sai CSRF | 403 | 403 | PASS |
| SEC06 mở chat thiếu CSRF | 403 | 403 | PASS |
| SEC07 mở chat sai CSRF | 403 | 403 | PASS |
| SEC08 admin POST thiếu CSRF | 403 | 403 | PASS |

Đã bổ sung user đăng nhập thật: GET products-crud/employees/statistics bị403, POST xử lý hóa đơn vớiCSRFhợp lệ bị403. Xem [bằng chứng](user-followup/README.md). Không suy rộng thành ma trận mọi route.

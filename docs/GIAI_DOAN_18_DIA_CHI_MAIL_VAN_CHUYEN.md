# GIAI ĐOẠN 18 – ĐỊA CHỈ KHÁCH, TÀI KHOẢN GỬI MAIL, BỎ THỜI GIAN GIAO HÀNG

Nhánh: `gop-y-sua-lan-2`. DB kiểm thử: `shopbangiay_kt_20261102_gd18_sua` (restore từ bản backup mới, không ghi đè DB có sẵn; restore lại sau mỗi lượt test phá dữ liệu). KHÔNG kết nối `shopbangiay` / `shopbangiay_thu`.

## Bảng kết quả

| Hạng mục | Cách kiểm tra | Kết quả thực tế | Trạng thái |
|---|---|---|---|
| A1. Nút "+ Thêm địa chỉ" ở form khách, tối đa 3 khối, khóa nút khi đủ 3 | Playwright `customer-create-multi-address` 1280px, `address-buttons-and-layout` 1280/375 | Thêm 2 lần → 3 khối, `addBtn.isDisabled() === true` | PASS |
| A2. Radio mặc định đúng 1, nút xóa khối | Chọn khối 2 làm mặc định rồi lưu, SELECT `dia_chi_nhan_hang` | 3 dòng, đúng 1 dòng `la_mac_dinh=1` (Đà Nẵng, thu_tu=2) | PASS |
| A3. Lưu khách + địa chỉ trong 1 transaction, `customers.address` = địa chỉ mặc định | SELECT `customers` id=101 | `address = "Số 20 Bạch Đằng, Đà Nẵng"` trùng địa chỉ mặc định | PASS |
| A4. Server chặn khối thứ 4 | JUnit (PeopleService/AdminCustomerController) | Trả `ShippingAddressService.LIMIT_MESSAGE` | PASS |
| A5. Chi tiết khách hiện bảng địa chỉ có cấu trúc, không còn `<dt>Địa chỉ</dt>` thô | Playwright `customer-detail-address-display` | 3 dòng bảng, không có dt "Địa chỉ" | PASS |
| A6. Form nhân viên giữ đúng 1 ô địa chỉ, không có nút thêm | Playwright `address-buttons-and-layout` | `[data-add-customer-address]` = 0, `#address` = 1 | PASS |
| B1. Mật khẩu tạm 8 ký tự `[A-Za-z0-9]`, ≥1 chữ ≥1 số, SecureRandom | JUnit `AccountCredentialPrivacyTest` + mail bắt bằng SMTP giả lập | `QPfa2B5Z`, `kBcEUYH6`, `TdjxW3NU` (dữ liệu test, đã restore DB) | PASS |
| B2. Mail tiếng Việt có tên đăng nhập, mật khẩu tạm, link đăng nhập | Đọc thân mail từ mock SMTP | Có `Mật khẩu tạm:` và `http://localhost:18318/login` | PASS |
| B3. Lần đăng nhập đầu buộc đổi mật khẩu | Playwright `login-generated-8char-and-force-change` | Chuyển `/account/change-required`, chặn `/profile`; đổi xong `must_change_password=0` | PASS |
| B4. "Gửi lại tài khoản" tạo mật khẩu mới và gửi lại | Playwright `resend-account-button` | Mật khẩu mới khác mật khẩu cũ, dài 8 | PASS |
| B5. SMTP lỗi không 500, không mất tài khoản | JUnit `AccountProvisioningIntegrationTest` | Tài khoản vẫn tạo, hiện cảnh báo `credentialMailFailed` | PASS |
| C1. Bỏ `/admin/shipping-times` (302) và menu "Thời gian giao hàng" | Playwright `shipping-config-layout-and-redirect` 1280/375 | 302 → `/admin/shipping-config`; sidebar có "Cấu hình vận chuyển", không có "Thời gian giao hàng" | PASS |
| C2. Trang "Cấu hình vận chuyển" (kho + nấc phí), Admin-only | Playwright + 8 kịch bản bảo mật | Có `#ship-setting-originAddress`, `#ship-setting-firstFee`, không có bảng vùng; nhân viên 403 | PASS |
| C3. Bỏ "Ngày giao dự kiến" ở hóa đơn, chi tiết đơn, tin nhắn sao chép | Playwright `no-delivery-estimate-in-invoices-and-orders` 1280/375, `user-order-detail-...` | Không có "Dự kiến giao"/"ngày giao dự kiến" | PASS |
| C4. Không DROP cột/bảng (`du_kien_giao_tu`, `du_kien_giao_den`, `vung_giao_hang`) | `ddl-auto=validate` khi khởi động | Khởi động sạch, không migration mới | PASS |
| rg tham chiếu cũ | `git grep shipping-times src/main` | Chỉ còn mapping 302 (`docs/evidence/gd18/rg-shipping-after.txt`); trước: 26 dòng (`rg-shipping-before.txt`) | PASS |
| mvnw clean test | `verify/gd18_sua/run.ps1 -Mode test` (không skip) | `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0` – BUILD SUCCESS (52.5 s) | PASS |
| Crawl link nội bộ | `crawl.py` | admin 459, nhân viên 204, user 22, khách 20 URL; 0 link chết, 0 thiếu CSRF, 0 tràn ngang | PASS |
| 8 kịch bản bảo mật | `security-scenarios` | s1 403, s2 403, s3 403, s4 403, s5 403, s6 302, s7 302, s8 403/302 | PASS |

## Bằng chứng SQL (sau khi tạo khách qua form)

```json
customer: {id:101, customer_code:"KH-GD18-A", address:"Số 20 Bạch Đằng, Đà Nẵng", user_id:589}
dia_chi_nhan_hang:
 {id:8, dia_chi:"Số 10 Tràng Tiền, Hà Nội", la_mac_dinh:false, thu_tu:1}
 {id:9, dia_chi:"Số 20 Bạch Đằng, Đà Nẵng", la_mac_dinh:true,  thu_tu:2}
 {id:10,dia_chi:"Số 30 Nguyễn Huệ, TP HCM",  la_mac_dinh:false, thu_tu:3}
```
Trước khi test: khách `KH-GD18-A` không tồn tại. Sau test DB được restore về checkpoint (`restore-checkpoint.log`).

Toàn bộ: `docs/evidence/gd18/gd18-sql-evidence.json`, `browser-results.json`, `crawl-manifest.json`, ảnh `gd18-*.png`.

## Chỗ đã thay đổi so với giai đoạn cũ
- GĐ12: mật khẩu tạm 12 ký tự có ký hiệu → 8 ký tự chữ+số.
- GĐ12: form khách 1 ô địa chỉ → tối đa 3 khối địa chỉ có cấu trúc; chi tiết khách bỏ dòng địa chỉ thô.
- GĐ13/15: trang "Thời gian giao hàng" (`/admin/shipping-times`) → "Cấu hình vận chuyển" (`/admin/shipping-config`), URL cũ 302.
- GĐ13: bỏ tính/hiển thị ngày giao dự kiến (`DeliveryService.snapshotEstimate/estimateText`), xóa `DeliveryServiceTest` (kiểm thử hàm đã bỏ, thay bằng kiểm tra trong `DeliveryIntegrationTest`).
- `DeliveryIntegrationTest`: thông báo kiểm tra vĩ độ đổi theo `@DecimalMax`.

## Cấu hình mail
Biến môi trường: `MAIL_HOST`, `MAIL_PORT`, `MAIL_AUTH`, `MAIL_STARTTLS`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `PUBLIC_BASE_URL`. Kiểm thử dùng SMTP giả lập `verify/gd18_sua/mock_smtp.py` (cổng 18325).

## Lệnh chạy
Không có migration mới ở giai đoạn này.
